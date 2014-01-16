package pals.plugins.marking;

import pals.base.Logging;
import pals.base.NodeCore;
import pals.base.Plugin;
import pals.base.Settings;
import pals.base.UUID;
import pals.base.database.Connector;
import pals.base.utils.JarIO;
import pals.base.utils.ThreadPool;

/**
 * A plugin for marking (instances of) assignments.
 */
public class AssignmentMarker extends Plugin
{
    // Fields ******************************************************************
    private final ThreadPool<MarkerThread> threads;     // The threads for processing/marking work.
    // Methods - Constructors **************************************************
    public AssignmentMarker(NodeCore core, UUID uuid, JarIO jario, Settings settings, String jarPath)
    {
        super(core, uuid, jario, settings, jarPath);
        this.threads = new ThreadPool<>();
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
    public boolean eventHandler_pluginLoad(NodeCore core)
    {
        // Setup thread-pool
        int numThreads = settings.getInt("marking/threads");
        getCore().getLogging().log("Ass. Marker", "Launching "+numThreads+" marking threads.", Logging.EntryType.Info);
        for(int i = 0; i < numThreads; i++)
            threads.add(new MarkerThread(this, i+1));
        // Start threads
        threads.start();
        return true;
    }
    @Override
    public void eventHandler_pluginUnload(NodeCore core)
    {
        getCore().getLogging().log("Ass. Marker", "Stopping thread-pool.", Logging.EntryType.Info);
        // Dispose threads
        threads.stopJoin();
        threads.clear();
        getCore().getLogging().log("Ass. Marker", "Disposed thread-pool.", Logging.EntryType.Info);
    }
    @Override
    public String getTitle()
    {
        return "[PALS] Assignment Marker";
    }
}
