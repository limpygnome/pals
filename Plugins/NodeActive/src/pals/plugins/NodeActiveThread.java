package pals.plugins;

import pals.base.Logging;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.utils.ExtendedThread;

/**
 * The class used to handle the updating of the database for this node.
 */
public class NodeActiveThread extends ExtendedThread
{
    // Fields ******************************************************************
    private NodeActive na;
    // Methods - Constructors **************************************************
    public NodeActiveThread(NodeActive na)
    {
        this.na = na;
    }
    // Methods *****************************************************************
    @Override
    public void run()
    {
        long interval = (long)na.getSettings().getInt("interval_ms");
        long lastUpdated = System.currentTimeMillis();
        Connector conn;
        while(!extended_isStopped())
        {
            // Sleep for a while to avoid excessive CPU usage
            try
            {
                Thread.sleep(interval);
            }
            catch(InterruptedException ex)
            {
                if(!extended_isStopped())
                    na.getCore().getLogging().logEx("NodeActive", "Unexpectedly woken.", ex, Logging.EntryType.Warning);
            }
            // Check if we need to update yet
            if(System.currentTimeMillis()-lastUpdated >= interval-100) // Allow 100 m/s early
            {
                lastUpdated = System.currentTimeMillis();
                // Update the database
                conn = na.getCore().createConnector();
                try
                {
                    if(conn == null)
                        throw new DatabaseException(DatabaseException.Type.ConnectionFailure);
                    conn.execute("UPDATE pals_nodes SET last_active=current_timestamp WHERE uuid_node=?;", na.getCore().getNodeUUID().getBytes());
                }
                catch(DatabaseException ex)
                {
                    na.getCore().getLogging().logEx("NodeActive", "Failed to update database.", ex, Logging.EntryType.Error);
                }
                // Disconnect from the database
                conn.disconnect();
            }
        }
    }
}
