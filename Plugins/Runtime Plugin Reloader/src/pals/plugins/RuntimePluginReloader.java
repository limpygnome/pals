package pals.plugins;

import java.io.File;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import pals.base.Logging;
import pals.base.NodeCore;
import pals.base.Plugin;
import pals.base.PluginManager;
import pals.base.Settings;
import pals.base.Storage;
import pals.base.TemplateItem;
import pals.base.TemplateManager;
import pals.base.UUID;
import pals.base.Version;
import pals.base.WebManager;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.utils.JarIO;
import pals.base.web.MultipartUrlParser;
import pals.base.web.RemoteRequest;
import pals.base.web.WebRequestData;
import pals.base.web.security.CSRF;
import pals.plugin_reloader.models.ModelPlugin;

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
    public RuntimePluginReloader(NodeCore core, UUID uuid, JarIO jario, Version version, Settings settings, String jarPath)
    {
        super(core, uuid, jario, version, settings, jarPath);
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
                            getCore().getLogging().logEx("RuntimePluginReloader", "Failed to load plugin - database exception.", ex, Logging.EntryType.Error);
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
            core.getLogging().logEx("RuntimePluginReloader", "Failed to start file monitor.", ex, Logging.EntryType.Error);
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
            core.getLogging().logEx("RuntimePluginReloader", "Failed to start templates monitor.", ex, Logging.EntryType.Error);
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
        // Dispose urls
        core.getWebManager().urlsUnregister(this);
        // Dispose templates
        core.getTemplates().remove(this);
    }
    @Override
    public boolean eventHandler_registerTemplates(NodeCore core, TemplateManager manager)
    {
        if(!manager.load(this, "templates"))
            return false;
        return true;
    }
    @Override
    public boolean eventHandler_registerUrls(NodeCore core, WebManager web)
    {
        if(!web.urlsRegister(this, new String[]{
            "admin/plugins"
        }))
            return false;
        return true;
    }
    @Override
    public boolean eventHandler_webRequest(WebRequestData data)
    {
        if(data.getUser() == null)
            return false;
        MultipartUrlParser mup = new MultipartUrlParser(data);
        String page = mup.getPart(0);
        if(page != null)
        {
            switch(page)
            {
                case "admin":
                    if(!data.getUser().getGroup().isAdminSystem())
                        return false;
                    page = mup.getPart(1);
                    if(page != null)
                    {
                        switch(page)
                        {
                            case "plugins":
                                return pageAdmin_plugins(data);
                        }
                    }
                    break;
            }
        }
        return false;
    }
    public boolean pageAdmin_plugins(WebRequestData data)
    {
        // Check for postback
        RemoteRequest req = data.getRequestData();
        String action = req.getField("action");
        String uuid = req.getField("uuid");
        // -- Optional
        String force = req.getField("force");
        if(action != null && uuid != null)
        {
            UUID pUUID = UUID.parse(uuid);
            ModelPlugin p;
            if(!CSRF.isSecure(data))
                data.setTemplateData("error", "Invalid request; please try again or contact an administrator.");
            else if((p=ModelPlugin.load(data.getConnector(), pUUID)) == null)
                return false;
            else if(pUUID != null)
            {
                switch(action)
                {
                    case "uninstall":
                        if(p.isSystem())
                            data.setTemplateData("error", "Cannot uninstall plugin; system flag is set.");
                        else
                        {
                            // Update state of plugin
                            ModelPlugin.pluginUninstall(data.getConnector(), p);
                            // Inform nodes to unload plugin
                            ModelPlugin.pluginUnload(data.getConnector(), p);
                            data.setTemplateData("success", "Successfully set plugin '"+p.getTitle()+"' ["+p.getUUID().getHexHyphens()+"] to uninstall.");
                        }
                        break;
                    case "delete":
                        if(p.isSystem())
                            data.setTemplateData("error", "Cannot delete plugin; system flag is set.");
                        else if((force == null || !force.equals("1")) && p.getState() != PluginManager.DbPluginState.Uninstalled.getDbVal())
                                data.setTemplateData("error", "Plugin must be uninstalled; use force-delete to ignore state safety check.");
                        else
                        {
                            // Remove from database
                            ModelPlugin.pluginDelete(data.getConnector(), p);
                            // Inform nodes to unload plugin
                            ModelPlugin.pluginUnload(data.getConnector(), p);
                            data.setTemplateData("success", "Successfully deleted plugin '"+p.getTitle()+"' ["+p.getUUID().getHexHyphens()+"].");
                        }
                        break;
                    case "unload":
                        ModelPlugin.pluginUnload(data.getConnector(), p);
                        data.setTemplateData("success", "Successfully unloaded plugin '"+p.getTitle()+"' ["+p.getUUID().getHexHyphens()+"] from all nodes.");
                        break;
                    default:
                        return false;
                }
            }
            else
                return false;
        }
        // Fetch plugins
        ModelPlugin[] plugins = ModelPlugin.load(data.getConnector());
        // Setup page
        data.setTemplateData("pals_title", "Modules");
        data.setTemplateData("pals_content", "plugin_reloader/plugins");
        data.setTemplateData("plugins", plugins);
        data.setTemplateData("csrf", CSRF.set(data));
        return true;
    }
    // Methods - Overrides *****************************************************
    @Override
    public String getTitle()
    {
        return "PALS: Runtime Plugin Reloader";
    }
}
