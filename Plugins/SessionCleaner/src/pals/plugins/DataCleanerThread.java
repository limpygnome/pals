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
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import pals.base.Logging;
import pals.base.Storage;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.database.Result;
import pals.base.utils.ExtendedThread;

/**
 * Responsible for cleaning-up old data.
 */
public class DataCleanerThread extends ExtendedThread
{
    // Fields ******************************************************************
    private SessionCleaner sc;
    // Methods - Constructors **************************************************
    public DataCleanerThread(SessionCleaner sc)
    {
        this.sc = sc;
    }
    // Methods - Overrides *****************************************************
    @Override
    public void run()
    {
        int interval = sc.getSettings().getInt("interval_data_ms", 3600000);
        String shared = sc.getCore().getPathShared();
        Connector conn = null;
        Result res;
        while(!extended_isStopped())
        {
            try
            {
                // Create connector
                if(conn == null)
                    conn = sc.getCore().createConnector();
                // Lock the table
                conn.tableLock("pals_cleanup", false);
                // Fetch work to do
                res = conn.read("SELECT * FROM pals_cleanup ORDER BY cid ASC LIMIT 1;");
                // Unlock table
                conn.tableUnlock(false);
                // Process work
                if(res.next())
                    cleanup(conn, shared, res);
                else
                {
                    conn = null;
                    // Sleep...
                    try
                    {
                        Thread.sleep(interval);
                    }
                    catch(InterruptedException ex)
                    {
                    }
                }
            }
            catch(DatabaseException ex)
            {
            }
        }
    }
    private void cleanup(Connector conn, String shared, Result res)
    {
        try
        {
            int id = (int)res.get("id");
            try
            {
                File t;
                switch((String)res.get("id_type"))
                {
                    case "1":
                        // IAQ
                        t = new File(Storage.getPath_tempIAQ(shared, id));
                        if(t.exists() && (long)conn.executeScalar("SELECT COUNT('') FROM pals_assignment_instance_question WHERE aiqid=?;", id) == 0)
                        {
                            FileUtils.deleteDirectory(t);
                            sc.getCore().getLogging().log(SessionCleaner.LOGGING_ALIAS, "Cleaned data for IAQ '"+id+"'.", Logging.EntryType.Info);
                        }
                        break;
                    case "2":
                        // Q
                        t = new File(Storage.getPath_tempQuestion(shared, id));
                        if(t.exists() && (long)conn.executeScalar("SELECT COUNT('') FROM pals_question WHERE qid=?;", id) == 0)
                        {
                            FileUtils.deleteDirectory(t);
                            sc.getCore().getLogging().log(SessionCleaner.LOGGING_ALIAS, "Cleaned data for Q '"+id+"'.", Logging.EntryType.Info);
                        }
                        break;
                    case "3":
                        // QC
                        t = new File(Storage.getPath_tempQC(shared, id));
                        if(t.exists() && (long)conn.executeScalar("SELECT COUNT('') FROM pals_question_criteria WHERE qcid=?;", id) == 0)
                        {
                            FileUtils.deleteDirectory(t);
                            sc.getCore().getLogging().log(SessionCleaner.LOGGING_ALIAS, "Cleaned data for QC '"+id+"'.", Logging.EntryType.Info);
                        }
                        break;
                }
            }
            catch(IOException ex)
            {
                sc.getCore().getLogging().logEx(SessionCleaner.LOGGING_ALIAS, ex, Logging.EntryType.Warning);
            }
            // Delete the job
            conn.execute("DELETE FROM pals_cleanup WHERE cid=?;", (int)res.get("cid"));
        }
        catch(DatabaseException ex)
        {
            sc.getCore().getLogging().logEx(SessionCleaner.LOGGING_ALIAS, ex, Logging.EntryType.Warning);
        }
    }
}
