package pals.plugins;

import java.io.File;
import java.io.FileNotFoundException;
import pals.base.Logging;
import pals.base.Storage;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.utils.ExtendedThread;
import pals.base.utils.Files;

/**
 * The thread used for performing cleanups.
 */
public class SessionCleanerThread extends ExtendedThread
{
    // Fields ******************************************************************
    private SessionCleaner sc;
    // Methods - Constructors **************************************************
    public SessionCleanerThread(SessionCleaner sc)
    {
        this.sc = sc;
    }
    // Methods - Overrides *****************************************************
    @Override
    public void run()
    {
        // Run periodically to cleanup temp files and session data
        long    interval = sc.getSettings().getInt("interval_ms"),
                sessionPublic = sc.getSettings().getInt("session_public_ms"),
                sessionPrivate = sc.getSettings().getInt("session_private_ms");
        long    lastRan = System.currentTimeMillis();
        Connector conn;
        while(!extended_isStopped())
        {
            // Check if to perform a cleanup
            if(System.currentTimeMillis()-lastRan >= interval-100) // Allow for 100 m/s early
            {
                lastRan = System.currentTimeMillis();
                // Cleanup the database
                conn = sc.getCore().createConnector();
                try
                {
                    if(conn == null)
                        throw new DatabaseException(DatabaseException.Type.ConnectionFailure);
                    conn.execute("DELETE FROM pals_http_sessions WHERE ((private='0' AND last_active < current_timestamp-CAST(? AS INTERVAL)) OR (private='1' AND last_active < current_timestamp-CAST(? AS INTERVAL)));", sessionPublic+" milliseconds", sessionPrivate+" milliseconds");
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
                        if(System.currentTimeMillis()-f.lastModified() >= sessionPublic)
                            f.delete();
                    }
                }
                catch(FileNotFoundException ex)
                {
                }
            }
            // Sleep for a while...
            try
            {
                sleep(interval);
            }
            catch(InterruptedException ex)
            {
                if(!extended_isStopped())
                    sc.getCore().getLogging().logEx(SessionCleaner.LOGGING_ALIAS, "Unexpectedly woken.", ex, Logging.EntryType.Error);
            }
        }
    }
}
