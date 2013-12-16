package pals.plugins;

import java.io.File;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import pals.base.Logging;
import pals.base.NodeCore;
import pals.base.Settings;
import pals.base.UUID;

/**
 * A plugin which monitors the file-system and automatically loads/reloads
 * plugins during runtime (especially useful for debugging/development).
 */
public class RuntimePluginReloader extends pals.base.Plugin
{
    // Fields ******************************************************************
    private FileAlterationObserver  filesObserver;
    private FileAlterationMonitor   filesMonitor;
    private FileAlterationListener  filesListener;
    // Methods - Constructors **************************************************
    public RuntimePluginReloader(NodeCore core, UUID uuid, Settings settings)
    {
        super(core, uuid, settings);
    }
    // Methods - Event Handlers ************************************************
    @Override
    public boolean eventHandler_pluginLoad(NodeCore core)
    {
        // Setup the plugins dir to be watched
        filesObserver = new FileAlterationObserver(core.getPathPlugins());
        // Setup the monitor w. interval
        filesMonitor = new FileAlterationMonitor((long)settings.getInt("interval_ms"));
        // Setup the anonymous class to handle changes
        filesListener = new FileAlterationListenerAdaptor()
            {
                @Override
                public void onFileChange(File file)
                {
                    reload(file.getPath());
                }
                @Override
                public void onFileCreate(File file)
                {
                    reload(file.getPath());
                }
                void reload(String path)
                {
                    if(path.endsWith(".jar"))
                    {
                        System.out.println("PALS - Runtime Plugin Reloader: file modified at '" + path + "'; loading into runtime...");
                        getCore().getPlugins().load(path);
                    }
                }
            };
        // Hook them all together
        filesObserver.addListener(filesListener);
        filesMonitor.addObserver(filesObserver);
        try
        {
            filesMonitor.start();
        }
        catch(Exception ex)
        {
            core.getLogging().log("Failed to start file monitor.", ex, Logging.EntryType.Error);
            return false;
        }
        return true;
    }
    @Override
    public void eventHandler_pluginUnload(NodeCore core)
    {
        // Dispose observer/monitor/listener
        try
        {
            filesMonitor.stop();
            filesMonitor = null;
            filesObserver = null;
            filesListener = null;
        }
        catch(Exception ex)
        {
        }
    }
    // Methods - Overrides *****************************************************
    @Override
    public String getTitle()
    {
        return "PALS: Runtime Plugin Reloader";
    }
}
