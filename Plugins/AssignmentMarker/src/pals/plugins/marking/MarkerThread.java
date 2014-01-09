package pals.plugins.marking;

import pals.base.Logging;
import pals.base.NodeCore;
import pals.base.Plugin;
import pals.base.UUID;
import pals.base.assessment.InstanceAssignment;
import pals.base.assessment.InstanceAssignmentCriteria;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.utils.ExtendedThread;

/**
 * A worker thread for finding and delegating instances of criteria to be
 * marked.
 */
public class MarkerThread extends ExtendedThread
{
    // Fields ******************************************************************
    private final AssignmentMarker marker;
    private final int number;
    // Methods - Constructors **************************************************
    public MarkerThread(AssignmentMarker marker, int number)
    {
        this.marker = marker;
        this.number = number;
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
                marker.getCore().waitStateChange();
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
        long lastRan        = System.currentTimeMillis()-interval;
        boolean flagWorked  = false;
        
        marker.getCore().getLogging().log("Ass. Marker", "Thread "+number+" started.", Logging.EntryType.Info);
        
        while(!extended_isStopped())
        {
            // Fail-safe against waking from sleep
            // -- If not the database would be attacked with the equivalent of a denial of service attack
            if(flagWorked || System.currentTimeMillis()-lastRan >=interval)
            {
                try
                {
                    flagWorked = processWork(conn, timeout);
                }
                catch(Exception ex)
                {
                    marker.getCore().getLogging().logEx("Ass. Marker", ex, Logging.EntryType.Error);
                    // Cool-down...
                    flagWorked = false;
                }
            }
            // Sleep...
            try
            {
                Thread.sleep(interval);
            }
            catch(InterruptedException ex)
            {
            }
        }
        // Dispose connector
        conn.disconnect();
        marker.getCore().getLogging().log("Ass. Marker", "Thread "+number+" ending execution.", Logging.EntryType.Info);
    }
    private synchronized boolean processWork(Connector conn, int timeout)
    {
        // Fetch work to do
        InstanceAssignmentCriteria iac = null;
        try
        {
            // Lock the table to fetch work
            conn.tableLock("pals_assignment_instance_question_criteria", false);
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
        if(iac != null)
        {
            // Delegate to the plugin responsible for marking
            UUID plugin = iac.getQC().getCriteria().getUuidPlugin();
            Plugin p = marker.getCore().getPlugins().getPlugin(plugin);
            if(p == null || !p.eventHandler_handleHook("criteria_type.mark", new Object[]{conn, iac}))
            {
                // Set to manual marking, log the error
                iac.setStatus(InstanceAssignmentCriteria.Status.AwaitingManualMarking);
                iac.persist(conn);
                if(p == null)
                    marker.getCore().getLogging().log("Ass. Marker", "Criteria-type plugin, "+plugin.getHexHyphens()+", is not loaded in the run-time.", Logging.EntryType.Warning);
                else
                    marker.getCore().getLogging().log("Ass. Marker", "Plugin, "+plugin.getHexHyphens()+", did not handle criteria-type, "+iac.getQC().getCriteria().getUuidCType().getHexHyphens()+".", Logging.EntryType.Warning);
                return false;
            }
            else
            {
                marker.getCore().getLogging().log("Ass. Marker", "Marked criteria '"+iac.getIAQ().getAIQID()+"','"+iac.getQC().getQCID()+"'.", Logging.EntryType.Info);
                // Check if the assignment needs the overall mark computed
                try
                {
                    boolean needsMarkComputed = false;
                    InstanceAssignment ia = iac.getIAQ().getInstanceAssignment();
                    conn.tableLock("pals_assignment_instance_question_criteria", false);
                    if(ia.isMarkComputationNeeded(conn))
                    {
                        // Set the assignment to 'Marking'
                        ia.setStatus(InstanceAssignment.Status.Marking);
                        if(ia.persist(conn) != InstanceAssignment.PersistStatus.Success)
                        {
                            marker.getCore().getLogging().log("Ass. Marker", "Failed to set instance-assignment to marked.", Logging.EntryType.Error);
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
                            marker.getCore().getLogging().log("Ass. Marker", "Failed to compute marks for assignment instance '"+ia.getAIID()+"'.", Logging.EntryType.Warning);
                        else
                        {
                            // Update the status
                            ia.setStatus(InstanceAssignment.Status.Marked);
                            ia.persist(conn);
                            marker.getCore().getLogging().log("Ass. Marker", "Computed marks for assignment instance '"+ia.getAIID()+"' ~ "+ia.getMark()+"%.", Logging.EntryType.Info);
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
