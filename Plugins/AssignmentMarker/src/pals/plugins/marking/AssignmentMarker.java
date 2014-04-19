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

import java.util.HashSet;
import java.util.concurrent.LinkedBlockingQueue;
import pals.base.Logging;
import pals.base.NodeCore;
import pals.base.Plugin;
import pals.base.PluginManager;
import pals.base.Settings;
import pals.base.UUID;
import pals.base.Version;
import pals.base.assessment.InstanceAssignment;
import pals.base.assessment.InstanceAssignmentCriteria;
import pals.base.database.Connector;
import pals.base.utils.JarIO;
import pals.base.utils.ThreadPool;

/**
 * A plugin for marking (instances of) assignments.
 * 
 * @version 1.0
 */
public class AssignmentMarker extends Plugin
{
    // Fields ******************************************************************
    private ThreadMain                                              threadMain;     // The thread for fetching work.
    private final ThreadPool<ThreadMarker>                          threadWorkers;  // The threads for processing/marking work.
    private final LinkedBlockingQueue<InstanceAssignmentCriteria>   work;           // Pending work.
    private final HashSet<InstanceAssignment>                       assMarkCompute; // Assignments requiring marking.
    // Methods - Constructors **************************************************
    public AssignmentMarker(NodeCore core, UUID uuid, JarIO jario, Version version, Settings settings, String jarPath)
    {
        super(core, uuid, jario, version, settings, jarPath);
        
        this.threadMain = new ThreadMain(this);
        this.threadWorkers = new ThreadPool<>();
        
        this.work = new LinkedBlockingQueue<>();
        this.assMarkCompute = new HashSet<>();
    }
    // Methods - Event Handlers ************************************************
    @Override
    public boolean eventHandler_pluginInstall(NodeCore core, Connector conn)
    {
        return true;
    }
    @Override
    public boolean eventHandler_pluginUninstall(NodeCore core, Connector conn)
    {
        return true;
    }
    @Override
    public boolean eventHandler_registerHooks(NodeCore core, PluginManager plugins)
    {
        if(!plugins.globalHookRegister(this, "base.assessment.wake"))
            return false;
        return true;
    }
    @Override
    public boolean eventHandler_handleHook(String event, Object[] args)
    {
        switch(event)
        {
            case "base.assessment.wake":
                // Wake main thread to fetch new work
                threadMain.interrupt();
                return true;
        }
        return false;
    }
    @Override
    public boolean eventHandler_pluginLoad(NodeCore core)
    {
        // Setup thread-pool
        int numThreads = settings.getInt("marking/threads");
        getCore().getLogging().log("Ass. Marker", "Launching "+numThreads+" marking threads.", Logging.EntryType.Info);
        for(int i = 0; i < numThreads; i++)
            threadWorkers.add(new ThreadMarker(this, i+1));
        // Start main thread
        threadMain.start();
        // Start worker threads
        threadWorkers.start();
        return true;
    }
    @Override
    public void eventHandler_pluginUnload(NodeCore core)
    {
        getCore().getLogging().log("Ass. Marker", "Stopping thread-pool.", Logging.EntryType.Info);
        // Dispose threads
        // -- Stop and join main thread
        threadMain.extended_stop();
        try
        {
            threadMain.join();
        }
        catch(InterruptedException ex) {}
        // -- Stop workers
        threadWorkers.stopJoin();
        threadWorkers.clear();
        getCore().getLogging().log("Ass. Marker", "Disposed thread-pool.", Logging.EntryType.Info);
        // Dispose work
        assMarkCompute.clear();
        work.clear();
    }
    // Methods - Accessors *****************************************************
    /**
     * @see Plugin#getTitle()
     * 
     * @since 1.0
     */
    @Override
    public String getTitle()
    {
        return "[PALS] Assignment Marker";
    }
    /**
     * Fetches the queue storing work to be processed by marker threads.
     * 
     * @return Queue.
     * @since 1.0
     */
    public LinkedBlockingQueue<InstanceAssignmentCriteria> getWorkQueue()
    {
        return work;
    }
    /**
     * Adds an instance of assignment to be checked for marking.
     * 
     * @param ia The instance assignment requiring checking.
     * @since 1.0
     */
    public void addInstanceAssignmentMarking(InstanceAssignment ia)
    {
        synchronized(assMarkCompute)
        {
            assMarkCompute.add(ia);
        }
        // Wake main thread to perform checking
        threadMain.interrupt();
    }
    /**
     * Fetches all of the instance assignments requiring mark computation to be
     * checked.
     * 
     * @return Array of IAs, can be empty.
     * @since 1.0
     */
    public InstanceAssignment[] fetchComputeCheckIAs()
    {
        synchronized(assMarkCompute)
        {
            InstanceAssignment[] buffer = assMarkCompute.toArray(new InstanceAssignment[assMarkCompute.size()]);
            assMarkCompute.clear();
            return buffer;
        }
    }
}
