package pals.plugins;

import java.io.File;
import java.io.FileNotFoundException;
import pals.base.Logging;
import pals.base.Storage;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.utils.Files;

/**
 * The thread used for performing cleanups.
 */
public class SessionCleanerThread extends Thread
{
    // Fields ******************************************************************
    private SessionCleaner sc;
    private boolean shouldRun;
    // Methods - Constructors **************************************************
    public SessionCleanerThread(SessionCleaner sc)
    {
        this.sc = sc;
    }
    // Methods - Overrides *****************************************************
    @Override
    public void run()
    {
        shouldRun = true;
        // Run periodically to cleanup temp files and session data
        long interval = sc.getSettings().getInt("interval_ms");
        long lastRan = System.currentTimeMillis();
        Connector conn;
        while(shouldRun)
        {
            // Sleep for a while...
            try
            {
                sleep(interval);
            }
            catch(InterruptedException ex)
            {
                if(shouldRun)
                    sc.getCore().getLogging().logEx("SessionCleaner", "Unexpectedly woken.", ex, Logging.EntryType.Error);
            }
            // Check if to perform a cleanup
            if(System.currentTimeMillis()-lastRan >= interval)
            {
                lastRan = System.currentTimeMillis();
                // Cleanup the database
                conn = sc.getCore().createConnector();
                try
                {
                    if(conn == null)
                        throw new DatabaseException(DatabaseException.Type.ConnectionFailure);
                    conn.execute("DELETE FROM pals_http_sessions WHERE last_active < current_timestamp-CAST(? AS INTERVAL);", interval+" milliseconds");
                }
                catch(DatabaseException ex)
                {
                    sc.getCore().getLogging().logEx("SessionCleaner", "Could not delete old database session data.", ex, Logging.EntryType.Error);
                }
                // Disconnect from database
                conn.disconnect();
                // Cleanup temp web files
                try
                {
                    File[] files = Files.getAllFiles(Storage.getPath_tempWeb(sc.getCore().getPathShared()), false, true, null, false);
                    for(File f : files)
                    {
                        if(System.currentTimeMillis()-f.lastModified() >= interval)
                            f.delete();
                    }
                }
                catch(FileNotFoundException ex)
                {
                }
            }
        }
    }
    // Methods *****************************************************************
    public void stopRunning()
    {
        shouldRun = false;
        interrupt();
    }
}
