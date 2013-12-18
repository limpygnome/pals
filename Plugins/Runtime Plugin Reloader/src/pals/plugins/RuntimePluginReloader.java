package pals.plugins;

import java.io.File;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import pals.base.Logging;
import pals.base.NodeCore;
import pals.base.Plugin;
import pals.base.Settings;
import pals.base.Storage;
import pals.base.TemplateItem;
import pals.base.UUID;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;

/**
 * A plugin which monitors the file-system and automatically loads/reloads
 * plugins during runtime (especially useful for debugging/development). It also
 * reloads templates from shared storage.
 */
public class RuntimePluginReloader extends pals.base.Plugin
{
    // Fields - Plugins ********************************************************
    private FileAlterationObserver  filesObserverPlugins;
    private FileAlterationMonitor   filesMonitorPlugins;
    private FileAlterationListener  filesListenerPlugins;
    // Fields - Templates ******************************************************
    private String                  pathTemplates;
    private FileAlterationObserver  filesObserverTemplates;
    private FileAlterationMonitor   filesMonitorTemplates;
    private FileAlterationListener  filesListenerTemplates;
    // Methods - Constructors **************************************************
    public RuntimePluginReloader(NodeCore core, UUID uuid, Settings settings, String jarPath)
    {
        super(core, uuid, settings, jarPath);
    }

    // Methods - Event Handlers ************************************************
    @Override
    public boolean eventHandler_pluginInstall(NodeCore core)
    {
        return true;
    }
    @Override
    public boolean eventHandler_pluginUninstall(NodeCore core)
    {
        return true;
    }
    @Override
    public boolean eventHandler_pluginLoad(NodeCore core)
    {
        pathTemplates = Storage.getPath_templates(core.getPathShared());
        // Setup the plugins dir to be watched
        filesObserverPlugins = new FileAlterationObserver(core.getPathPlugins());
        filesObserverTemplates = new FileAlterationObserver(pathTemplates);
        // Setup the monitor w. interval
        long interval = (long)settings.getInt("interval_ms");
        filesMonitorPlugins = new FileAlterationMonitor(interval);
        filesMonitorTemplates = new FileAlterationMonitor(interval);
        // Setup the anonymous class to handle changes
        filesListenerPlugins = new FileAlterationListenerAdaptor()
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
                        try
                        {
                            // Create connector
                            Connector conn = getCore().createConnector();
                            if(conn == null)
                                throw new DatabaseException(DatabaseException.Type.ConnectionFailure);
                            // Reload plugin
                            getCore().getPlugins().load(conn, path);
                            // Dispose connector
                            conn.disconnect();
                        }
                        catch(DatabaseException ex)
                        {
                            getCore().getLogging().log("[RuntimePluginReloader] Failed to load plugin - database exception.", ex, Logging.EntryType.Error);
                        }
                    }
                }
            };
        filesListenerTemplates = new FileAlterationListenerAdaptor()
            {
                @Override
                public void onFileChange(File file)
                {
                    templateReload(file.getPath());
                }
                @Override
                public void onFileCreate(File file)
                {
                    templateReload(file.getPath());
                }
                @Override
                public void onFileDelete(File file)
                {
                    templateDelete(file.getPath());
                }
                void templateReload(String filePath)
                {
                    if(filePath.endsWith(".template"))
                    {
                        String path = parsePath(filePath);
                        // Locate the existing owner of the template - else we'll take ownership...
                        TemplateItem item = getCore().getTemplates().getTemplate(path);
                        Plugin owner = item != null ? getCore().getPlugins().getPlugin(item.getPluginUUID()) : null;
                        // Reload into the collection
                        getCore().getTemplates().loadFile(owner != null ? owner : RuntimePluginReloader.this, filePath, path);
                    }
                }
                void templateDelete(String filePath)
                {
                    if(filePath.endsWith(".template"))
                        getCore().getTemplates().remove(parsePath(filePath));
                }
                String parsePath(String filePath)
                {
                    return filePath.substring(pathTemplates.length()+1, filePath.length()-9); // +1 due to slash, 9 = length of .template
                }
            };
        // Hook listener/observer
        filesObserverPlugins.addListener(filesListenerPlugins);
        filesMonitorPlugins.addObserver(filesObserverPlugins);
        filesObserverTemplates.addListener(filesListenerTemplates);
        filesMonitorTemplates.addObserver(filesObserverTemplates);
        // Begin plugins monitor
        try
        {
            filesMonitorPlugins.start();
        }
        catch(Exception ex)
        {
            core.getLogging().log("[RuntimePluginReloader] Failed to start file monitor.", ex, Logging.EntryType.Error);
            return false;
        }
        // Begin template monitor
        try
        {
            filesMonitorTemplates.start();
        }
        catch(Exception ex)
        {
            // Stop the plugins monitor - we're going into failure mode...
            try
            {
                filesMonitorPlugins.stop();
            }
            catch(Exception ex2)
            {
            }
            core.getLogging().log("[RuntimePluginReloader] Failed to start templates monitor.", ex, Logging.EntryType.Error);
            return false;
        }
        return true;
    }
    @Override
    public void eventHandler_pluginUnload(NodeCore core)
    {
        // Dispose observer/monitor/listener for plugins
        try
        {
            filesMonitorPlugins.stop();
            filesMonitorPlugins = null;
            filesObserverPlugins = null;
            filesListenerPlugins = null;
        }
        catch(Exception ex)
        {
        }
        // Dispose observer/monitor/listener for templates
        try
        {
            filesMonitorTemplates.stop();
            filesMonitorTemplates = null;
            filesObserverTemplates = null;
            filesListenerTemplates = null;
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
