package pals.base;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.database.Result;
import pals.base.utils.Files;
import pals.base.utils.JarIO;
import pals.base.utils.JarIOException;

/**
 * Manages all the plugins loaded into the runtime, as well as general plugin
 * operations.
 * 
 * Allows plugins to be assigned to global hooks, which can be used to retrieve
 * a list of plugins associated with a hook/event. This allows open-ended events
 * for e.g. processing work and handling the start/end of web-requests.
 * 
 * Thread-safe.
 * *****************************************************************************
 * Base hooks:
 * base.web.request_start           WebRequestData        Invoked at the start of a web-request.
 * base.web.request_end             WebRequestData        Invoked at the end of a web-request.
 * base.web.request_404             WebRequestData        Invoked to handle page not found event.
 */
public class PluginManager
{
    // Enums *******************************************************************
    /**
     * The state of the plugin, held in the database.
     */
    public enum DbPluginState
    {
        /**
         * Indicates the state is unknown.
         */
        Unknown(0),
        /**
         * Indicates the plugin is pending installation.
         */
        PendingInstall(1),
        /**
         * Indicates the plugin has been installed.
         */
        Installed(2),
        /**
         * Indicates the plugin is pending uninstallation.
         */
        PendingUninstall(4),
        /**
         * Indicates the plugin has been uninstalled; such a plugin will be
         * rejected from loading into the runtime.
         */
        Uninstalled(8);
        
        private int val;    // The value state in the database.
        private DbPluginState(int val)
        {
            this.val = val;
        }
        public static DbPluginState getType(int value)
        {
            switch(value)
            {
                case 1:
                    return PendingInstall;
                case 2:
                    return Installed;
                case 4:
                    return PendingUninstall;
                case 8:
                    return Uninstalled;
                default:
                    return Unknown;
            }
        }
    }
    /**
     * The status from attempting to load a plugin.
     */
    public enum PluginLoad
    {
        /**
         * Indicates the JAR has been loaded as a plugin.
         */
        Loaded,
        /**
         * Indicates the JAR could not be loaded because it's not a plugin or
         * has invalid plugin configuration.
         */
        FailedIrrelevant,
        /**
         * Indicates the plugin has been uninstalled; thus it has been rejected
         * from loading into the runtime.
         */
        FailedRejected,
        /**
         * Indicates the JAR is most likely a plugin, since it contains a
         * configuration file, but could not be loaded.
         */
        Failed
    }
    // Fields ******************************************************************
    private NodeCore                            core;                   // The current instance of the core.
    private HashMap<String,ArrayList<Plugin>>   registerGlobalEvents;   // Global registered events; <event name,<list of plugins>>.
    private HashMap<UUID,Plugin>                plugins;                // The plugins of the runtime.
    // Methods - Constructors **************************************************
    protected PluginManager(NodeCore core)
    {
        this.core = core;
        this.registerGlobalEvents = new HashMap<>();
        this.plugins = new HashMap<>();
    }
    // Methods *****************************************************************
    /**
     * Re-registers all the global events.
     * 
     * @return True if successful, false if failed.
     */
    public synchronized boolean globalHookRegisterAll()
    {
        boolean allSuccess = true;
        // Clear old events
        registerGlobalEvents.clear();
        // Invoke each plugin to register events
        for(Map.Entry<UUID,Plugin> plugin : plugins.entrySet())
        {
            if(!plugin.getValue().eventHandler_registerHooks(core, this))
                allSuccess = false;
        }
        return allSuccess;
    }
    /**
     * Registers a plugin to a global event; this is used by the base, but it
     * can also be used by other plugins for their own cross-plugin global
     * events system.
     * 
     * @param plugin The plugin to be invoked when the event occurs.
     * @param event The name of the event.
     * @return True if subscribed, false if failed (most likely already
     * subscribed).
     */
    public synchronized boolean globalHookRegister(Plugin plugin, String event)
    {
        // Check the event has an array-list
        if(!registerGlobalEvents.containsKey(event))
            registerGlobalEvents.put(event, new ArrayList<Plugin>());
        // Grab the list of plugins already hooked
        ArrayList<Plugin> pgs = registerGlobalEvents.get(event);
        // Add the plugin if it doesnt exist
        if(!pgs.contains(plugin))
        {
            pgs.add(plugin);
            return true;
        }
        else
            return false;
    }
    /**
     * Unregisters a global hook for a plugin.
     * 
     * @param plugin The plugin of the event to unregister.
     * @param event The event to unregister.
     */
    public synchronized void globalHookUnregister(Plugin plugin, String event)
    {
        // Fetch arraylist of event
        ArrayList<Plugin> pgs = registerGlobalEvents.get(event);
        // Remove event
        if(pgs != null)
            pgs.remove(plugin);
        // Remove the event list if it's now empty - waste of storage and potentially processing...
        if(pgs.isEmpty())
            registerGlobalEvents.remove(event);
    }
    /**
     * Unregisters all global hooks associated with a plugin.
     * 
     * @param plugin The plugin of the events to unregister.
     */
    public synchronized void globalHookUnregister(Plugin plugin)
    {
        ArrayList<Plugin> pgs;
        Iterator<Map.Entry<String,ArrayList<Plugin>>> it = registerGlobalEvents.entrySet().iterator();
        Map.Entry<String,ArrayList<Plugin>> p;
        while(it.hasNext())
        {
            p = it.next();
            pgs = p.getValue();
            // Remove plugin
            pgs.remove(plugin);
            // Remove the list if it's now empty
            if(pgs.isEmpty())
                it.remove();
        }
    }
    /**
     * Reloads all of the plugins from the path specified in the current instance
     * of the NodeCore.
     * 
     * @param conn Database connector.
     * @return True if successful, false if failed.
     */
    public synchronized boolean reload(Connector conn)
    {
        // Unload all the existing plugins
        {
            Plugin[] pgs = getPlugins();
            for(Plugin p : pgs)
                unload(p);
        }
        // Attempt to load each JAR in the plugins directory
        try
        {
            core.getLogging().log("[PLUGINS] Loading plugins at '" + core.getPathPlugins() + "'...", Logging.EntryType.Info);
            for(File jar : Files.getAllFiles(core.getPathPlugins(), false, true, ".jar", true))
            {
                if(load(conn, jar.getPath()) == PluginLoad.Failed)
                    return false;
            }
        }
        catch(FileNotFoundException ex)
        {
            core.getLogging().log(ex, Logging.EntryType.Error);
            return false;
        }
        return true;
    }
    /**
     * Loads a JAR (Java Archive) plugin into the runtime.
     * 
     * Note:
     * - If a plugin with the same UUID is already loaded, that plugin is
     *   unloaded.
     * 
     * @param conn Database connector.
     * @param jarPath The path of the plugin.
     * @return True = successful, false = failed.
     */
    public synchronized PluginLoad load(Connector conn, String jarPath)
    {
        try
        {
            File filePath = new File(jarPath);
            // Load the original JAR
            JarIO jar = JarIO.open(jarPath, null);
            // Read configuration file
            String xml = jar.fetchFileText("plugin.config");
            Settings ps = Settings.loadXml(xml, true);
            // Read UUID and check it's valid
            String rawUuid = ps.getStr("plugin/uuid");
            UUID uuid = UUID.parse(rawUuid);
            if(uuid == null)
            {
                core.getLogging().log("[PLUGINS] Failed to load plugin at '" + jarPath + "' - incorrect UUID '" + rawUuid + "'.", Logging.EntryType.Error);
                jar.dispose();
                return PluginLoad.Failed;
            }
            // Check the UUID is not already in-use
            if(plugins.containsKey(uuid))
            {
                // Unload the old plugin
                Plugin p = plugins.get(uuid);
                if(p == null || !unload(p))
                {
                    core.getLogging().log("[PLUGINS] Failed to load plugin at '" + jarPath + "' ~ plugin [" + uuid.getHexHyphens() + "] ~ already loaded; could not be unloaded..", Logging.EntryType.Error);
                    jar.dispose();
                    return PluginLoad.Failed;
                }
            }
            // Build list of files (relative paths)
            String pathOriginal = filePath.getParent();
            String pathNew = core.getPathPlugins_Temp() + "/" + uuid.getHexHyphens();
            File filePathOriginal = new File(pathOriginal);
            // -- Open-ended in-case of future changes
            ArrayList<String> fileDependencies = new ArrayList<>(); // new file paths of dependencies
            HashMap<String,String> files = new HashMap<>(); // original path,dest path
            files.put(filePath.getName(), "plugin.jar");
            {
                // Add plugin dependencies
                String pluginDependencies = ps.getStr("plugin/plugin_dependencies");
                if(pluginDependencies != null)
                {
                    UUID dp;
                    for(String s : pluginDependencies.split(","))
                    {
                        if((dp = UUID.parse(s)) != null)
                            fileDependencies.add(core.getPathPlugins_Temp() + "/" + dp.getHexHyphens() + "/plugin.jar");
                        else
                        {
                            core.getLogging().log("[PLUGINS] Failed to load plugin at '" + jarPath + "' ~ plugin [" + uuid.getHexHyphens() + "] ~ invalid plugin dependency '"+s+"' - must be a UUID!", Logging.EntryType.Error);
                            jar.dispose();
                            return PluginLoad.Failed;
                        }
                    }
                }
                // Add dependencies
                String dependencies = ps.getStr("plugin/dependencies");
                if(dependencies != null)
                {
                    // Iterate each dependency
                    for(String d : dependencies.split(","))
                    {
                        if(d.endsWith("*"))
                        {
                            // Relative directory - add all the files
                            try
                            {
                                String n; // -- used to compute the relative path to the base
                                for(File f : Files.getAllFiles(pathOriginal + "/" + d.substring(0, d.length()-2), false, true, null, true))
                                {
                                    n = filePathOriginal.toURI().relativize(f.toURI()).getPath();
                                    files.put(n,n);
                                    fileDependencies.add(n);
                                }
                            }
                            catch(FileNotFoundException ex)
                            {
                                core.getLogging().log("Failed to load files of dependency path '"+d+"'; plugin at '"+jarPath+"' [" + uuid.getHexHyphens() + "].", ex, Logging.EntryType.Error);
                                jar.dispose();
                                return PluginLoad.Failed;
                            }
                        }
                        else
                        {
                            files.put(d, d);
                            fileDependencies.add(d);
                        }
                    }
                }
            }
            // Dispose jar
            jar.dispose();
            // Copy list of files
            for(Map.Entry<String,String> kv : files.entrySet())
            {
                try
                {
                    FileUtils.copyFile(new File(pathOriginal + "/" + kv.getKey()), new File(pathNew + "/" + kv.getValue()));
                }
                catch(IOException ex)
                {
                    core.getLogging().log("Failed to copy plugin file '"+kv.getKey()+"' to '"+kv.getValue()+"' plugin at '"+jarPath+"' [" + uuid.getHexHyphens() + "].", ex, Logging.EntryType.Error);
                    return PluginLoad.Failed;
                }
            }
            // Open jar at new location
            jar = JarIO.open(pathNew + "/plugin.jar", fileDependencies.toArray(new String[fileDependencies.size()]));
            // Fetch the plugin class and load an instance into the runtime
            String classPath = ps.getStr("plugin/classpath");
            if(classPath == null)
            {
                core.getLogging().log("[PLUGINS] Failed to load plugin at '" + jarPath + "' - no class-path specified.", Logging.EntryType.Error);
                jar.dispose();
                return PluginLoad.Failed;
            }
            Class c = jar.fetchClassType(classPath);
            // -- Create a new instance
            Plugin p = (Plugin)c.getDeclaredConstructor(NodeCore.class, UUID.class, JarIO.class, Settings.class, String.class).newInstance(core, uuid, jar, ps, jarPath);
            // Check the state of the plugin
            try
            {
                // Lock the plugins table - in-case this plugin is loaded at the same time on another node
                // -- http://www.postgresql.org/docs/current/static/explicit-locking.html
                conn.tableLock("pals_plugins", false);
                // Retrieve the state
                boolean failed = false, rejected = false, exists = false;
                Result res = conn.read("SELECT state FROM pals_plugins WHERE uuid_plugin=?;", p.getUUID().getBytes());
                DbPluginState s = DbPluginState.PendingInstall;
                if(res.next())
                {
                    exists = true;
                    // Check the state
                    s = DbPluginState.getType((int)res.get("state"));
                }
                // Handle state
                DbPluginState newState = s;
                switch(s)
                {
                    case Installed:
                        // Do nothing...we will continue loading the plugin as normal...
                        break;
                    case PendingInstall:
                        if(!exists)
                        {
                            // Create plugin record
                            conn.execute("INSERT INTO pals_plugins (uuid_plugin, title, state, system) VALUES(?,?,?,?);", p.getUUID().getBytes(), p.getTitle(), DbPluginState.PendingInstall.val, p.isSystemPlugin() ? "1" : "0");
                            core.getLogging().log("[PLUGINS] Plugin '" + p.getTitle() + "' [" + uuid.getHexHyphens() + "] - added to the database.", Logging.EntryType.Info);
                        }
                        // Run installation of plugin
                        if(!p.eventHandler_pluginInstall(core))
                        {
                            core.getLogging().log("[PLUGINS] Plugin '" + p.getTitle() + "' [" + uuid.getHexHyphens() + "] - failed to install!", Logging.EntryType.Error);
                            failed = true;
                        }
                        else
                        {
                            core.getLogging().log("[PLUGINS] Plugin '" + p.getTitle() + "' [" + uuid.getHexHyphens() + "] - installed.", Logging.EntryType.Info);
                            newState = DbPluginState.Installed;
                        }
                        break;
                    case PendingUninstall:
                        // Run uninstallation of plugin
                        if(!p.eventHandler_pluginUninstall(core))
                        {
                            core.getLogging().log("[PLUGINS] Plugin '" + p.getTitle() + "' [" + uuid.getHexHyphens() + "] - failed to uninstall!", Logging.EntryType.Error);
                            failed = true;
                        }
                        else
                        {
                            core.getLogging().log("[PLUGINS] Plugin '" + p.getTitle() + "' [" + uuid.getHexHyphens() + "] - uninstalled.", Logging.EntryType.Info);
                            newState = DbPluginState.Uninstalled;
                            rejected = true;
                        }
                        break;
                    case Uninstalled:
                        rejected = true;
                        break;
                    case Unknown:
                        failed = true;
                        break;
                }
                // Update the state (if it has changed)
                if(newState != s)
                    conn.execute("UPDATE pals_plugins SET state=? WHERE uuid_plugin=?;", newState.val, uuid.getBytes());
                // Unlock the table - if a connection issue occurred, the locks associated with the connection would be droped
                conn.tableUnlock(false);
                if(rejected)
                {
                    jar.dispose();
                    return PluginLoad.FailedRejected;
                }
                else if(failed)
                {
                    jar.dispose();
                    return PluginLoad.Failed;
                }
            }
            catch(DatabaseException ex)
            {
                core.getLogging().log("[PLUGINS] Plugin '" + p.getTitle() + "' [" + uuid.getHexHyphens() + "] - failed to retrieve/update state from database!", ex, Logging.EntryType.Error);
                jar.dispose();
                return PluginLoad.Failed;
            }
            // Add the plugin to the runtime
            plugins.put(uuid, p);
            // Inform the plugin to register to global events and templates/template-functions, urls and that's being loaded into the runtime
            if(!p.eventHandler_registerHooks(core, this))
            {
                core.getLogging().log("[PLUGINS] Plugin '" + p.getTitle() + "' [" + uuid.getHexHyphens() + "] - failed to register global event hooks!", Logging.EntryType.Error);
                unload(p);
                jar.dispose();
                return PluginLoad.Failed;
            }
            else if(!p.eventHandler_registerTemplates(core, core.getTemplates()))
            {
                core.getLogging().log("[PLUGINS] Plugin '" + p.getTitle() + "' [" + uuid.getHexHyphens() + "] - failed to register templates/template-functions!", Logging.EntryType.Error);
                unload(p);
                jar.dispose();
                return PluginLoad.Failed;
            }
            else if(!p.eventHandler_registerUrls(core, core.getWebManager()))
            {
                core.getLogging().log("[PLUGINS] Plugin '" + p.getTitle() + "' [" + uuid.getHexHyphens() + "] - failed to register paths/urls!", Logging.EntryType.Error);
                unload(p);
                jar.dispose();
                return PluginLoad.Failed;
            }
            else if(!p.eventHandler_pluginLoad(core))
            {
                core.getLogging().log("[PLUGINS] Plugin '" + p.getTitle() + "' [" + uuid.getHexHyphens() + "] - failed to load!", Logging.EntryType.Error);
                unload(p);
                jar.dispose();
                return PluginLoad.Failed;
            }
            core.getLogging().log("[PLUGINS] Loaded plugin '" + p.getTitle() + "' ('" + jarPath + "')[" + uuid.getHexHyphens() + "].", Logging.EntryType.Info);
            return PluginLoad.Loaded;
        }
        catch(SettingsException ex)
        {
            core.getLogging().log("[PLUGINS] Failed to load plugin at '" + jarPath + "' - incorrect settings file.", ex, Logging.EntryType.Error);
            return PluginLoad.FailedIrrelevant;
        }
        catch(JarIOException ex)
        {
            switch(ex.getReason())
            {
                default:
                    //core.getLogging().log("[PLUGINS] Failed to load potential JAR at '" + jarPath + "'.", ex, Logging.EntryType.Warning);
                    return PluginLoad.FailedIrrelevant;
                case ClassNotFound:
                    core.getLogging().log("[PLUGINS] Failed to load class of plugin at '" + jarPath + "'.", ex, Logging.EntryType.Error);
                    break;
            }
        }
        catch(InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException ex)
        {
            core.getLogging().log("[PLUGINS] Invalid class for plugin at '" + jarPath + "'.", ex, Logging.EntryType.Error);
        }
        return PluginLoad.Failed;
    }
    /**
     * Unloads a plugin from the runtime.
     * 
     * @param plugin The plugin to be removed from the runtime.
     * @return True = unloaded, false = not found/not unloaded.
     */
    public synchronized boolean unload(Plugin plugin)
    {
        // Check the plugin is valid
        if(plugin == null || !(plugins.containsKey(plugin.getUUID()) && plugins.containsValue(plugin)))
            return false;
        // Unload the plugin from the runtime
        plugin.eventHandler_pluginUnload(core);
        // Remove from manager
        plugins.remove(plugin.getUUID());
        // Dispose I/O
        plugin.getJarIO().dispose();
        core.getLogging().log("[PLUGINS] Unloaded plugin '" + plugin.getTitle() + "' (" + plugin.getUUID().getHexHyphens() + ").", Logging.EntryType.Info);
        return true;
    }
    /**
     * Unloads all of the plugins.
     */
    public synchronized void unload()
    {
        for(Plugin p : getPlugins())
            unload(p);
    }
    // Methods - Accessors *****************************************************
    /**
     * Fetches a plugin by its identifier.
     * 
     * @param uuid The unique universal identifier of the plugin.
     * @return The current instance of the plugin or null.
     */
    public synchronized Plugin getPlugin(UUID uuid)
    {
        return plugins.get(uuid);
    }
    /**
     * @return All of the active plugins in the runtime.
     */
    public synchronized Plugin[] getPlugins()
    {
        return plugins.values().toArray(new Plugin[plugins.size()]);
    }
    /**
     * @param event The name of the global event.
     * @return All of the plugins registered to an event or an empty array.
     */
    public synchronized Plugin[] getPlugins(String event)
    {
        ArrayList<Plugin> result = registerGlobalEvents.get(event);
        return result == null ? new Plugin[0] : result.toArray(new Plugin[result.size()]);
    }
}
