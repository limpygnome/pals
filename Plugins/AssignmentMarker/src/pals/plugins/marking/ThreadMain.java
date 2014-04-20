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
    Authors:    Marcus Craske           <limpygnome@gmail.com>
    ----------------------------------------------------------------------------
*/
package pals.plugins.marking;

import java.util.ArrayList;
import pals.base.Logging;
import pals.base.NodeCore;
import pals.base.assessment.Assignment;
import pals.base.assessment.InstanceAssignment;
import pals.base.assessment.InstanceAssignmentCriteria;
import pals.base.assessment.Module;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.database.Result;
import pals.base.utils.ExtendedThread;

/**
 * Performs any fetching and computation of assignments. This is the only
 * single point, in the plugin, where locking should occur.
 * 
 * @version 1.0
 */
public class ThreadMain extends ExtendedThread
{
    // Constants ***************************************************************
    private final String LOCK_TABLE = "pals_node_locking";
    // Fields ******************************************************************
    private AssignmentMarker am;
    // Methods - Constructors **************************************************
    /**
     * Constructs a new instance.
     * 
     * @param am The instance of the plugin.
     * @since 1.0
     */
    public ThreadMain(AssignmentMarker am)
    {
        this.am = am;
    }
    /**
     * @see Thread#run()
     * 
     * @since 1.0
     */
    @Override
    public void run()
    {
        // Wait for the core to start before starting...
        // -- We need all the plugins to load in...
        while(am.getCore().getState() == NodeCore.State.Starting && !extended_isStopped())
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
        
        // Create connection
        Connector conn = am.getCore().createConnector();
        if(conn == null)
            return;
        
        // Fetch settings
        int interval        = am.getSettings().getInt("marking/poll_interval", 10000),
            timeout         = am.getSettings().getInt("marking/work_timeout", 120000),
            fetchRate       = am.getSettings().getInt("marking/fetch_rate", 16);
        
        am.getCore().getLogging().log("Ass. Marker", "Main thread started.", Logging.EntryType.Info);
        
        boolean flagWorked;
        while(!extended_isStopped())
        {
            // Attempt to process work
            try
            {
                flagWorked = processedWork(conn, timeout, fetchRate);
            }
            catch(Exception ex)
            {
                am.getCore().getLogging().logEx("Ass. Marker", "Failed to process work.", ex, Logging.EntryType.Error);
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
        am.getCore().getLogging().log("Ass. Marker", "Main thread ending execution.", Logging.EntryType.Info);
    }
    
    private boolean processedWork(Connector conn, int timeout, int fetchRate)
    {
        boolean hasWorked = false;
        // Check for unhandled surpassed assignments
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
                am.getCore().getLogging().log("Ass. Marker", "Assignment '"+ass.getAssID()+"' has surpassed due-date; auto-submitting.", Logging.EntryType.Info);
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
            am.getCore().getLogging().logEx("Ass. Marker", "Main Thread : Failed to handle due assignments.", ex, Logging.EntryType.Error);
        }
        // Check if instance of assignments need mark computation
        InstanceAssignment[] iarr = am.fetchComputeCheckIAs();
        if(iarr.length > 0)
        {
            ArrayList<InstanceAssignment> needComputing = new ArrayList<>();
            try
            {
                // Lock table
                conn.tableLock(LOCK_TABLE, false);
                // Iterate each IA, check if needs computing
                for(InstanceAssignment ia : iarr)
                {
                    if(ia.isMarkComputationNeeded(conn))
                    {
                        // Update status to being marked
                        ia.setStatus(InstanceAssignment.Status.Marking);
                        
                        if(ia.persist(conn) != InstanceAssignment.PersistStatus.Success)
                            am.getCore().getLogging().log("Ass. Marker", "Failed to set instance-assignment to marked.", Logging.EntryType.Error);
                        else
                            // Add to buffer for marks to be computed
                            needComputing.add(ia);
                    }
                }
                // Unlock table
                conn.tableUnlock(false);
            }
            catch(DatabaseException ex)
            {
            }
            // Compute marks for IAs
            for(InstanceAssignment ia : needComputing)
            {
                if(!ia.computeMark(conn))
                    am.getCore().getLogging().log("Ass. Marker", "Failed to compute marks for assignment instance '"+ia.getAIID()+"'.", Logging.EntryType.Warning);
                else
                {
                    // Update the status
                    ia.setStatus(InstanceAssignment.Status.Marked);
                    ia.persist(conn);
                    am.getCore().getLogging().log("Ass. Marker", "Computed marks for assignment instance '"+ia.getAIID()+"' ~ "+ia.getMark()+"%.", Logging.EntryType.Info);
                }
            }
        }
        // Fetch work to do - but only if the queue is not at the fetch-rate
        if(am.getWorkQueue().size() < fetchRate)
        {
            try
            {
                // Lock the table to fetch work
                conn.tableLock(LOCK_TABLE, false);
                // Fetch work
                InstanceAssignmentCriteria[] newWork = InstanceAssignmentCriteria.loadNextWork(am.getCore(), conn, timeout, fetchRate-am.getWorkQueue().size());
                // Add to queue
                if(newWork.length > 0)
                {
                    for(InstanceAssignmentCriteria iac : newWork)
                    {
                        // Ensure item is unique
                        if(!am.getWorkQueue().contains(iac))
                            am.getWorkQueue().add(iac);
                    }
                }
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
        }
        return hasWorked;
    }
}
