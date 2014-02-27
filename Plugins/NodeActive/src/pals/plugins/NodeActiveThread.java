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

import pals.base.Logging;
import pals.base.NodeCore;
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
        // Wait for the core to start...
        while(!extended_isStopped() && na.getCore().getState() != NodeCore.State.Started)
        {
            try
            {
                Thread.sleep(5);
            }
            catch(InterruptedException ex) {}
        }
        // Begin updating the status
        long interval = (long)na.getSettings().getInt("interval_ms");
        long lastUpdated = 0;
        Connector conn;
        while(!extended_isStopped())
        {
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
                // Update local cache of hosts
                na.getCore().getRMI().hostsUpdate(conn);
                // Disconnect from the database
                conn.disconnect();
            }
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
        }
    }
}
