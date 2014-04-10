/*
    The MIT License (MIT)

    Copyright (c) 2014 Marcus Craske <limpygnome@gmail.com>

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in
    all copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
    THE SOFTWARE.
    ----------------------------------------------------------------------------
    Version:    1.0
    Authors:    Marcus Craske           <limpygnome@gmail.com>
    ----------------------------------------------------------------------------
*/
package pals.plugins.marking;

import pals.base.Logging;
import pals.base.NodeCore;
import pals.base.Plugin;
import pals.base.UUID;
import pals.base.assessment.Assignment;
import pals.base.assessment.InstanceAssignment;
import pals.base.assessment.InstanceAssignmentCriteria;
import pals.base.assessment.Module;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.database.Result;
import pals.base.utils.ExtendedThread;

/**
 * A worker thread for finding and delegating instances of criteria to be
 * marked.
 */
public class MarkerThread extends ExtendedThread
{
    // Constants ***************************************************************
    private final String LOCK_TABLE = "pals_node_locking";
    // Fields ******************************************************************
    private final AssignmentMarker  marker; // Reference to the plugin.
    private final int               number; // The # / number of this thread (for diagnostics/debugging).
    private final NodeCore          core;
    // Methods - Constructors **************************************************
    public MarkerThread(AssignmentMarker marker, int number)
    {
        this.marker = marker;
        this.number = number;
        this.core = marker.getCore();
    }
    // Methods *****************************************************************
    @Override
    public void run()
    {
        // Wait for the core to start before starting...
        // -- We need all the plugins to load in...
        while(marker.getCore().getState() == NodeCore.State.Starting && !extended_isStopped())
        {
            try
            {
                //marker.getCore().waitStateChange();
                Thread.sleep(100);
            }
            catch(InterruptedException ex)
            {
            }
        }
        if(extended_isStopped())
        {
            extended_reset();
            return;
        }
        // Create a connector
        Connector conn = marker.getCore().createConnector();
        if(conn == null)
        {
            return;
        }
        int interval        = marker.getSettings().getInt("marking/poll_interval", 10000);
        int timeout         = marker.getSettings().getInt("marking/work_timeout", 120000);
        boolean flagWorked  = false;
        
        marker.getCore().getLogging().log("Ass. Marker", "Thread "+number+" started.", Logging.EntryType.Info);
        
        while(!extended_isStopped())
        {
            // Attempt to fetch and process the next available piece of work...
            try
            {
                flagWorked = processWork(conn, timeout);
            }
            catch(Exception ex)
            {
                marker.getCore().getLogging().logEx("Ass. Marker", "Failed to process work ~ thread "+number+".", ex, Logging.EntryType.Error);
                // Cool-down...
                flagWorked = false;
            }
            // No work occurred; sleep...
            if(!flagWorked)
            {
                try
                {
                    Thread.sleep(interval);
                }
                catch(InterruptedException ex)
                {
                }
            }
        }
        // Dispose connector
        conn.disconnect();
        marker.getCore().getLogging().log("Ass. Marker", "Thread "+number+" ending execution.", Logging.EntryType.Info);
    }
    private boolean processWork(Connector conn, int timeout)
    {
        // Process assignments where the due-date has been surpassed and unhandled
        try
        {
            conn.execute("BEGIN;");
            conn.tableLock(LOCK_TABLE, true);
            // Fetch any unhandled surpassed assignments
            Result res = conn.read("SELECT * FROM pals_assignment WHERE due_handled='0' AND due IS NOT NULL AND due < current_timestamp;");
            Result res2;
            Module m;
            Assignment ass;
            while(res.next())
            {
                // Load assignment model
                ass = Assignment.load(conn, null, res);
                marker.getCore().getLogging().log("Ass. Marker", "#"+number+": Assignment '"+ass.getAssID()+"' has surpassed due-date; auto-submitting.", Logging.EntryType.Info);
                // Update the assignment to handled
                conn.execute("UPDATE pals_assignment SET due_handled='1' WHERE assid=?;", ass.getAssID());
                // Iterate each assignment, create criteria and submit
                InstanceAssignment ia;
                res2 = conn.read("SELECT * FROM pals_assignment_instance WHERE status=? AND assid=?;", InstanceAssignment.Status.Active.getStatus(), ass.getAssID());
                while(res2.next())
                {
                    ia = InstanceAssignment.load(conn, ass, null, res2);
                    if(ia != null)
                    {
                        // Update status to submitted
                        ia.setStatus(InstanceAssignment.Status.Submitted);
                        ia.persist(conn);
                        // Create criteria
                        InstanceAssignmentCriteria.createForInstanceAssignment(conn, ia, InstanceAssignmentCriteria.Status.AwaitingMarking);
                    }
                }
            }
            conn.tableUnlock(true);
            conn.execute("COMMIT;");
        }
        catch(DatabaseException ex)
        {
            try
            {
                conn.execute("ROLLBACK;");
            }
            catch(DatabaseException ex2)
            {
            }
            marker.getCore().getLogging().logEx("Ass. Marker", "#"+number+": Failed to handle due assignments.", ex, Logging.EntryType.Error);
        }
        // Fetch work to do
        InstanceAssignmentCriteria iac = null;
        try
        {
            // Lock the table to fetch work
            conn.tableLock(LOCK_TABLE, false);
            // Fetch the next piece of work to do
            iac = InstanceAssignmentCriteria.loadNextWork(marker.getCore(), conn, timeout);
            // Unlock the table
            conn.tableUnlock(false);
        }
        catch(DatabaseException ex)
        {
            try
            {
                conn.tableUnlock(false);
            }
            catch(DatabaseException ex2)
            {
            }
        }
        // Process (instance) assignment criterias which need marking
        if(iac != null)
        {
            // Delegate to the plugin responsible for marking
            UUID plugin = iac.getQC().getCriteria().getUuidPlugin();
            Plugin p = marker.getCore().getPlugins().getPlugin(plugin);
            if(p == null || !p.eventHandler_handleHook("criteria_type.mark", new Object[]{conn, core, iac}))
            {
                // Set to manual marking, log the error
                iac.setStatus(InstanceAssignmentCriteria.Status.AwaitingManualMarking);
                iac.persist(conn);
                if(p == null)
                    marker.getCore().getLogging().log("Ass. Marker", "#"+number+": Criteria-type plugin, "+plugin.getHexHyphens()+", is not loaded in the run-time.", Logging.EntryType.Warning);
                else
                    marker.getCore().getLogging().log("Ass. Marker", "#"+number+": Plugin, "+plugin.getHexHyphens()+", did not handle criteria-type, "+iac.getQC().getCriteria().getUuidCType().getHexHyphens()+".", Logging.EntryType.Warning);
                return false;
            }
            else
            {
                marker.getCore().getLogging().log("Ass. Marker", "#"+number+": Marked criteria '"+iac.getIAQ().getAIQID()+"','"+iac.getQC().getQCID()+"' ~ "+iac.getMark()+"%.", Logging.EntryType.Info);
                // Check if the assignment needs the overall mark computed
                try
                {
                    boolean needsMarkComputed = false;
                    InstanceAssignment ia = iac.getIAQ().getInstanceAssignment();
                    conn.tableLock(LOCK_TABLE, false);
                    if(ia.isMarkComputationNeeded(conn))
                    {
                        // Set the assignment to 'Marking' - allows us to free the lock-table whilst computing the mark
                        ia.setStatus(InstanceAssignment.Status.Marking);
                        if(ia.persist(conn) != InstanceAssignment.PersistStatus.Success)
                        {
                            marker.getCore().getLogging().log("Ass. Marker", "#"+number+": Failed to set instance-assignment to marked.", Logging.EntryType.Error);
                            return true;
                        }
                        else
                            needsMarkComputed = true;
                    }
                    conn.tableUnlock(false);
                    // Compute the assignment's overall mark...
                    if(needsMarkComputed)
                    {
                        // Compute the grade for the assignment
                        if(!ia.computeMark(conn))
                            marker.getCore().getLogging().log("Ass. Marker", "#"+number+": Failed to compute marks for assignment instance '"+ia.getAIID()+"'.", Logging.EntryType.Warning);
                        else
                        {
                            // Update the status
                            ia.setStatus(InstanceAssignment.Status.Marked);
                            ia.persist(conn);
                            marker.getCore().getLogging().log("Ass. Marker", "#"+number+": Computed marks for assignment instance '"+ia.getAIID()+"' ~ "+ia.getMark()+"%.", Logging.EntryType.Info);
                        }
                    }
                }
                catch(DatabaseException ex)
                {
                }
            }
            return true;
        }
        return false;
    }
}
