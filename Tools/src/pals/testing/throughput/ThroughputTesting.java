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
package pals.testing.throughput;

import java.io.File;
import pals.base.NodeCore;
import pals.base.assessment.InstanceAssignmentCriteria;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.testing.throughput.data.Regex;

/**
 * An application for performing through-put testing.
 * 
 * @version 1.0
 */
public class ThroughputTesting
{
    /**
     * Program entry-point.
     * 
     * @param args Environment args.
     * @since 1.0
     */
    public static void main(String[] args) throws DatabaseException
    {
        // Change this variable to control the amount of data tested
        final int data = 500;
        
        // Fetch and start an instance of a core, used for communication and DB
        // -- Create fake plugins folder, we do not want any plugins
        File f = new File("tt_plugins");
        if(!f.exists())
            f.mkdir();
        // -- Create core
        NodeCore core = NodeCore.getInstance();
        core.setPathPlugins("tt_plugins");
        core.setPathSettings("../Node/_config/node.config");
        // -- Start
        if(!core.start())
        {
            System.err.println("Failed to start instance of core.");
            return;
        }
        // Create database connector
        Connector conn = core.createConnector();
        // Lock nodes table
        conn.tableLock("pals_nodes", false);
        // Create fake data
        System.out.println("Creating test data...");
        TestData td = new Regex(data);
        td.create(core);
        // Log start time, unlock table
        long start = System.currentTimeMillis();
        conn.tableUnlock(false);
        // Inform all nodes of data to be marked
        core.getRMI().nodesGlobalEventAll("base.assessment.wake", null);
        // Poll table every 5 m/s for completion of test
        System.out.println("Waiting for work to be marked...");
        boolean hasCompleted = false;
        do
        {
            if((long)conn.executeScalar("SELECT COUNT('') FROM pals_assignment_instance_question_criteria WHERE status=?;", InstanceAssignmentCriteria.Status.AwaitingMarking.getStatus())==0L)
                hasCompleted=true;
        }
        while(!hasCompleted);
        long end = System.currentTimeMillis();
        // Output time taken
        System.out.println("Time taken (ms): "+(end-start));
        // Lock table
        conn.tableLock("pals_nodes", false);
        // Dispose data
        td.dispose(core);
        // Unlock table
        conn.tableUnlock(false);
        // Dispose core
        conn.disconnect();;
        core.stop();
    }
}
