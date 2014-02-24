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
