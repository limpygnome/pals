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
 * 
 * @version 1.0
 */
public class PluginManager
{
    // Enums *******************************************************************
    /**
     * The state of the plugin, held in the database.
     * 
     * @since 1.0
     */
    public enum DbPluginState
    {
        /**
         * Indicates the state is unknown.
         * 
         * @since 1.0
         */
        Unknown(0),
        /**
         * Indicates the plugin is pending installation.
         * 
         * @since 1.0
         */
        PendingInstall(1),
        /**
         * Indicates the plugin has been installed.
         * 
         * @since 1.0
         */
        Installed(2),
        /**
         * Indicates the plugin is pending uninstallation.
         * 
         * @since 1.0
         */
        PendingUninstall(4),
        /**
         * Indicates the plugin has been uninstalled; such a plugin will be
         * rejected from loading into the runtime.
         * 
         * @since 1.0
         */
        Uninstalled(8);
        
        private int val;    // The value state in the database.
        private DbPluginState(int val)
        {
            this.val = val;
        }
        
        /**
         * Fetches an enum type based on value.
         * 
         * @param value The value of the plugin state.
         * @return The enum representation; this is the unknown state if
         * an enum cannot match the value.
         * @since 1.0
         */
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
        /**
         * The value used on the database, for representing the state.
         * 
         * @return The value of the state for persistence on the database.
         * @since 1.0
         */
        public int getDbVal()
        {
            return val;
        }
    }
    /**
     * The status from attempting to load a plugin.
     * 
     * @since 1.0
     */
    public enum PluginLoad
    {
        /**
         * Indicates the JAR has been loaded as a plugin.
         * 
         * @since 1.0
         */
        Loaded,
        /**
         * Indicates the JAR could not be loaded because it's not a plugin or
         * has invalid plugin configuration.
         * 
         * @since 1.0
         */
        FailedIrrelevant,
        /**
         * Indicates the plugin has been uninstalled; thus it has been rejected
         * from loading into the runtime.
         * 
         * @since 1.0
         */
        FailedRejected,
        /**
         * Indicates the JAR is most likely a plugin, since it contains a
         * configuration file, but could not be loaded.
         * 
         * @since 1.0
         */
        Failed
    }
    // Fields - Constants ******************************************************
    private static final String                 LOGGING_ALIAS = "PALS Plugin Man.";
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
    // Methods - Hooks *********************************************************
    /**
     * Re-registers all the global events.
     * 
     * @return True if successful, false if failed.
     * @since 1.0
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
     * @since 1.0
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
     * @since 1.0
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
     * @since 1.0
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
     * Fetches all of the plugins hooked to an event.
     * 
     * @param event The name of the global event.
     * @return All of the plugins registered to an event, or an empty array.
     * @since 1.0
     */
    public synchronized Plugin[] globalHookFetch(String event)
    {
        ArrayList<Plugin> result = registerGlobalEvents.get(event);
        return result == null ? new Plugin[0] : result.toArray(new Plugin[result.size()]);
    }
    /**
     * Invokes all hooks registered to an event until a hook returns true.
     * 
     * @param event The name of the global event.
     * @param data The data to be passed to plugins.
     * @return True = a plugin handled the event, false = no plugins,
     * subscribed, have handled the event.
     * @since 1.0
     */
    public synchronized boolean globalHookInvoke(String event, Object[] data)
    {
        Plugin[] plugins = globalHookFetch(event);
        for(Plugin p : plugins)
            if(p.eventHandler_handleHook(event, data))
                return true;
        return false;
    }
    /**
     * Invokes all hooks registered to an event.
     * 
     * @param event The name of the global event.
     * @param data The data to be passed to plugins.
     * @since 1.0
     */
    public synchronized void globalHookInvokeAll(String event, Object[] data)
    {
        Plugin[] plugins = globalHookFetch(event);
        for(Plugin p : plugins)
            p.eventHandler_handleHook(event, data);
    }
    // Methods - Reloading *****************************************************
    /**
     * Reloads all of the plugins from the path specified in the current instance
     * of the NodeCore.
     * 
     * @param conn Database connector.
     * @return True if successful, false if failed.
     * @since 1.0
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
            core.getLogging().log(LOGGING_ALIAS, "Loading plugins at '" + core.getPathPlugins() + "'...", Logging.EntryType.Info);
            for(File jar : Files.getAllFiles(core.getPathPlugins(), false, true, ".jar", true))
            {
                if(load(conn, jar.getPath()) == PluginLoad.Failed)
                    return false;
            }
        }
        catch(FileNotFoundException ex)
        {
            core.getLogging().logEx(LOGGING_ALIAS, ex, Logging.EntryType.Error);
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
     * @since 1.0
     */
    public synchronized PluginLoad load(Connector conn, String jarPath)
    {
        try
        {
            File filePath = new File(jarPath);
            // Check the file is not within a dir called lib - this could be a library plugin, so ignore...
            try
            {
                if(filePath.getParentFile().getCanonicalPath().replace("\\", "/").endsWith("/lib"))
                    return PluginLoad.FailedIrrelevant;
            }
            catch(IOException ex)
            {
                return PluginLoad.Failed;
            }
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
                core.getLogging().log(LOGGING_ALIAS, "Failed to load plugin at '" + jarPath + "' - incorrect UUID '" + rawUuid + "'.", Logging.EntryType.Error);
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
                    core.getLogging().log(LOGGING_ALIAS, "Failed to load plugin at '" + jarPath + "' ~ plugin [" + uuid.getHexHyphens() + "] ~ already loaded; could not be unloaded..", Logging.EntryType.Error);
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
                            core.getLogging().log(LOGGING_ALIAS, "Failed to load plugin at '" + jarPath + "' ~ plugin [" + uuid.getHexHyphens() + "] ~ invalid plugin dependency '"+s+"' - must be a UUID!", Logging.EntryType.Error);
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
                                core.getLogging().logEx(LOGGING_ALIAS, "Failed to load files of dependency path '"+d+"'; plugin at '"+jarPath+"' [" + uuid.getHexHyphens() + "].", ex, Logging.EntryType.Error);
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
                    core.getLogging().logEx(LOGGING_ALIAS, "Failed to copy plugin file '"+kv.getKey()+"' to '"+kv.getValue()+"' plugin at '"+jarPath+"' [" + uuid.getHexHyphens() + "].", ex, Logging.EntryType.Error);
                    return PluginLoad.Failed;
                }
            }
            // Parse version
            Version version;
            try
            {
                version = new Version(ps.getInt("plugin/version/major"), ps.getInt("plugin/version/minor"), ps.getInt("plugin/version/build"));
            }
            catch(IllegalArgumentException ex)
            {
                core.getLogging().logEx(LOGGING_ALIAS, "Failed to parse version data for plugin at '"+jarPath+"' [" + uuid.getHexHyphens() + "].", ex, Logging.EntryType.Error);
                return PluginLoad.Failed;
            }
            // Open jar at new location
            jar = JarIO.open(pathNew + "/plugin.jar", fileDependencies.toArray(new String[fileDependencies.size()]));
            // Check we loaded the jar
            if(jar == null)
            {
                core.getLogging().log(LOGGING_ALIAS, "Failed to load jar at '" + jarPath + "'.", Logging.EntryType.Warning);
                return PluginLoad.FailedIrrelevant;
            }
            // Fetch the plugin class and load an instance into the runtime
            String classPath = ps.getStr("plugin/classpath");
            if(classPath == null || classPath.length() == 0)
            {
                core.getLogging().log(LOGGING_ALIAS, "Failed to load plugin at '" + jarPath + "' - no class-path specified.", Logging.EntryType.Warning);
                jar.dispose();
                return PluginLoad.FailedIrrelevant;
            }
            Class c = jar.fetchClassType(classPath);
            // -- Create a new instance
            Plugin p = (Plugin)c.getDeclaredConstructor(NodeCore.class, UUID.class, JarIO.class, Version.class, Settings.class, String.class).newInstance(core, uuid, jar, version, ps, jarPath);
            p.newJarLocation = pathNew + "/plugin.jar";
            // Check the state of the plugin
            try
            {
                // Lock the plugins table - in-case this plugin is loaded at the same time on another node
                // -- http://www.postgresql.org/docs/current/static/explicit-locking.html
                conn.tableLock("pals_plugins", false);
                // Retrieve the state
                boolean failed = false, rejected = false, exists = false;
                Result res = conn.read("SELECT state, version_major, version_minor, version_build FROM pals_plugins WHERE uuid_plugin=?;", p.getUUID().getBytes());
                DbPluginState s = DbPluginState.PendingInstall;
                if(res.next())
                {
                    exists = true;
                    // Check the state
                    s = DbPluginState.getType((int)res.get("state"));
                    // Parse database version
                    Version dbv;
                    try
                    {
                        dbv = new Version((int)res.get("version_major"), (int)res.get("version_minor"), (int)res.get("version_build"));
                    }
                    catch(IllegalArgumentException ex)
                    {
                        core.getLogging().log(LOGGING_ALIAS, "Failed to load plugin at '" + jarPath + "' - could not parse database version.", Logging.EntryType.Error);
                        jar.dispose();
                        return PluginLoad.Failed;
                    }
                    // Check the version is correct, if it exists
                    if(!dbv.equals(version))
                    {
                        core.getLogging().log(LOGGING_ALIAS, "Failed to load plugin at '" + jarPath + "' - version is different; expected '"+dbv+"', current '"+version+"'.", Logging.EntryType.Error);
                        jar.dispose();
                        return PluginLoad.Failed;
                    }
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
                            conn.execute("INSERT INTO pals_plugins (uuid_plugin, title, state, system, version_major, version_minor, version_build) VALUES(?,?,?,?,?,?,?);", p.getUUID().getBytes(), p.getTitle(), DbPluginState.PendingInstall.val, p.isSystemPlugin() ? "1" : "0", version.getMajor(), version.getMinor(), version.getBuild());
                            core.getLogging().log(LOGGING_ALIAS, "Plugin '" + p.getTitle() + "' [" + uuid.getHexHyphens() + "] - added to the database.", Logging.EntryType.Info);
                        }
                        // Run installation of plugin
                        if(!p.eventHandler_pluginInstall(core, conn))
                        {
                            core.getLogging().log(LOGGING_ALIAS, "Plugin '" + p.getTitle() + "' [" + uuid.getHexHyphens() + "] - failed to install!", Logging.EntryType.Error);
                            failed = true;
                        }
                        else
                        {
                            core.getLogging().log(LOGGING_ALIAS, "Plugin '" + p.getTitle() + "' [" + uuid.getHexHyphens() + "] - installed.", Logging.EntryType.Info);
                            newState = DbPluginState.Installed;
                        }
                        break;
                    case PendingUninstall:
                        // Run uninstallation of plugin
                        if(!p.eventHandler_pluginUninstall(core, conn))
                        {
                            core.getLogging().log(LOGGING_ALIAS, "Plugin '" + p.getTitle() + "' [" + uuid.getHexHyphens() + "] - failed to uninstall!", Logging.EntryType.Error);
                            failed = true;
                        }
                        else
                        {
                            core.getLogging().log(LOGGING_ALIAS, "Plugin '" + p.getTitle() + "' [" + uuid.getHexHyphens() + "] - uninstalled.", Logging.EntryType.Info);
                            newState = DbPluginState.Uninstalled;
                            rejected = true;
                        }
                        break;
                    case Uninstalled:
                        rejected = true;
                        // Invoke plugin's local uninstall handler
                        // -- Catch any exceptions; this plugin is no longer critical.
                        try
                        {
                            p.eventHandler_pluginUninstallLocal(core, conn);
                        }
                        catch(Exception ex)
                        {
                            core.getLogging().logEx(LOGGING_ALIAS, ex, Logging.EntryType.Warning);
                        }
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
                core.getLogging().logEx(LOGGING_ALIAS, "Plugin '" + p.getTitle() + "' [" + uuid.getHexHyphens() + "] - failed to retrieve/update state from database!", ex, Logging.EntryType.Error);
                jar.dispose();
                return PluginLoad.Failed;
            }
            // Add the plugin to the runtime
            plugins.put(uuid, p);
            // Inform the plugin to register to global events and templates/template-functions, urls and that's being loaded into the runtime
            if(!p.eventHandler_registerHooks(core, this))
            {
                core.getLogging().log(LOGGING_ALIAS, "Plugin '" + p.getTitle() + "' [" + uuid.getHexHyphens() + "] - failed to register global event hooks!", Logging.EntryType.Error);
                unload(p);
                jar.dispose();
                return PluginLoad.Failed;
            }
            else if(!p.eventHandler_registerTemplates(core, core.getTemplates()))
            {
                core.getLogging().log(LOGGING_ALIAS, "Plugin '" + p.getTitle() + "' [" + uuid.getHexHyphens() + "] - failed to register templates/template-functions!", Logging.EntryType.Error);
                unload(p);
                jar.dispose();
                return PluginLoad.Failed;
            }
            else if(!p.eventHandler_registerUrls(core, core.getWebManager()))
            {
                core.getLogging().log(LOGGING_ALIAS, "Plugin '" + p.getTitle() + "' [" + uuid.getHexHyphens() + "] - failed to register paths/urls!", Logging.EntryType.Error);
                unload(p);
                jar.dispose();
                return PluginLoad.Failed;
            }
            else if(!p.eventHandler_pluginLoad(core))
            {
                core.getLogging().log(LOGGING_ALIAS, "Plugin '" + p.getTitle() + "' [" + uuid.getHexHyphens() + "] - failed to load!", Logging.EntryType.Error);
                unload(p);
                jar.dispose();
                return PluginLoad.Failed;
            }
            core.getLogging().log(LOGGING_ALIAS, "Loaded plugin '" + p.getTitle() + "' ('" + jarPath + "')[" + uuid.getHexHyphens() + "][v"+version+"].", Logging.EntryType.Info);
            return PluginLoad.Loaded;
        }
        catch(SettingsException ex)
        {
            core.getLogging().logEx(LOGGING_ALIAS, "Failed to load plugin at '" + jarPath + "' - incorrect settings file.", ex, Logging.EntryType.Error);
            return PluginLoad.FailedIrrelevant;
        }
        catch(JarIOException ex)
        {
            switch(ex.getReason())
            {
                default:
                    // Not currently logged to avoid log-spam from third-party library jars
                    return PluginLoad.FailedIrrelevant;
                case ClassNotFound:
                    core.getLogging().logEx(LOGGING_ALIAS, "Failed to load class of plugin at '" + jarPath + "'.", ex, Logging.EntryType.Error);
                    break;
            }
        }
        catch(InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException ex)
        {
            core.getLogging().logEx(LOGGING_ALIAS, "Invalid class for plugin at '" + jarPath + "'.", ex, Logging.EntryType.Error);
        }
        return PluginLoad.Failed;
    }
    /**
     * Unloads a plugin from the runtime.
     * 
     * @param plugin The plugin to be removed from the runtime.
     * @return True = unloaded, false = not found/not unloaded.
     * @since 1.0
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
        JarIO jio = plugin.getJarIO();
        if(jio != null)
            jio.dispose();
        core.getLogging().log(LOGGING_ALIAS, "Unloaded plugin '" + plugin.getTitle() + "' (" + plugin.getUUID().getHexHyphens() + ").", Logging.EntryType.Info);
        return true;
    }
    /**
     * Unloads all of the plugins.
     * 
     * @since 1.0
     */
    public synchronized void unload()
    {
        for(Plugin p : getPlugins())
            unload(p);
    }
    /**
     * Adds a plugin to the manager.
     * 
     * NOTE: THIS IS ETXREMELY DANGEROUS AND SHOULD NOT BE USED, EXCEPT IN
     * TESTING CONDITIONS. This may be useful for plugins requiring multiple
     * instances, from a single JAR, loaded during runtime. It's advised you
     * do not use this method, as no checking occurs and the system could be
     * easily damaged. If you use this method, be absolutely sure your code
     * is safe and your plugin has all the required constructor variables
     * fulfilled.
     * 
     * NOTE 2: if a plugin exists with the same UUID, its entry WILL be
     * replaced. This could be extremely dangerous!
     * 
     * @param plugin The plugin to add.
     * @return Indicates if the plugin has been added. Basic UUID checking is
     * conducted.
     * @since 1.0
     */
    public synchronized boolean add(Plugin plugin)
    {
        UUID uuid = plugin.getUUID();
        if(uuid == null)
            return false;
        plugins.put(uuid, plugin);
        return true;
    }
    // Methods - Accessors *****************************************************
    /**
     * Fetches a plugin by its identifier.
     * 
     * @param uuid The unique universal identifier of the plugin.
     * @return The current instance of the plugin or null.
     * @since 1.0
     */
    public synchronized Plugin getPlugin(UUID uuid)
    {
        return plugins.get(uuid);
    }
    /**
     * Fetches all of the active plugins loaded in the current runtime.
     * 
     * @return Array of plugins, can be empty.
     * @since 1.0
     */
    public synchronized Plugin[] getPlugins()
    {
        return plugins.values().toArray(new Plugin[plugins.size()]);
    }
}
