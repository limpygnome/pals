package pals.plugins;

import pals.base.Logging;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;

/**
 * The class used to handle the updating of the database for this node.
 */
public class NodeActiveThread implements Runnable
{
    // Fields ******************************************************************
    private NodeActive na;
    private boolean run;
    // Methods - Constructors **************************************************
    public NodeActiveThread(NodeActive na)
    {
        this.na = na;
    }
    // Methods *****************************************************************
    public void run()
    {
        run = true;
        long interval = (long)na.getSettings().getInt("interval_ms");
        long lastUpdated = System.currentTimeMillis();
        Connector conn;
        while(run)
        {
            // Sleep for a while to avoid excessive CPU usage
            try
            {
                Thread.sleep(interval);
            }
            catch(InterruptedException ex)
            {
                if(run)
                    na.getCore().getLogging().log("[NodeActive] Unexpectedly woken.", ex, Logging.EntryType.Warning);
            }
            // Check if we need to update yet
            if(System.currentTimeMillis()-lastUpdated >= interval)
            {
                lastUpdated = System.currentTimeMillis();
                // Update the database
                try
                {
                    conn = na.getCore().createConnector();
                    conn.execute("UPDATE pals_nodes SET last_active=current_timestamp WHERE uuid_node=?;", na.getCore().getNodeUUID().getBytes());
                }
                catch(DatabaseException ex)
                {
                    na.getCore().getLogging().log("[NodeActive] Failed to update database.", ex, Logging.EntryType.Warning);
                }
            }
        }
    }
    public void stop()
    {
        run = false;
    }
}
