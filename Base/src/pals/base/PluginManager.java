package pals.base;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import pals.base.utils.Files;
import pals.base.utils.JarIO;
import pals.base.utils.JarIOException;

/**
 * Manages all the plugins loaded into the runtime, as well as general plugin
 * operations.
 * 
 * Thread-safe.
 */
public class PluginManager
{
    // Enums *******************************************************************
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
     * Registers a plugin to a global event; this is used by the base, but it
     * can also be used by other plugins for their own cross-plugin global
     * events system.
     * 
     * @param plugin The plugin to be invoked when the event occurs.
     * @param event The name of the event.
     * @return True if subscribed, false if failed (most likely already
     * subscribed).
     */
    public synchronized boolean registerGlobalEvent(Plugin plugin, String event)
    {
        ArrayList<Plugin> plugins;
        // Grab the list of plugins already hooked
        if(registerGlobalEvents.containsKey(event))
            plugins = registerGlobalEvents.get(event);
        else
            plugins = registerGlobalEvents.put(event, new ArrayList<Plugin>());
        // Add the plugin if it doesnt exist
        if(!plugins.contains(plugin))
            plugins.add(plugin);
        else
            return false;
        return true;
    }
    /**
     * Re-registers all the global events.
     * 
     * @return True if successful, false if failed.
     */
    public synchronized boolean registerAllEvents()
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
     * Loads all of the plugins from the path specified in the current instance
     * of the NodeCore.
     * 
     * @return True if successful, false if failed.
     */
    public synchronized boolean load()
    {
        // Attempt to load each JAR in the plugins directory
        try
        {
            for(File jar : Files.getAllFiles(core.getPathPlugins(), false, true, ".jar", true))
            {
                if(load(jar.getPath()) == PluginLoad.Failed)
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
     * @param jarPath The path of the plugin.
     * @return True = successful, false = failed.
     */
    public synchronized PluginLoad load(String jarPath)
    {
        try
        {
            // Load the JAR
            JarIO jar = JarIO.open(jarPath);
            // Read configuration file
            String xml = jar.fetchFileText("plugin.config");
            Settings ps = Settings.loadXml(xml, true);
            // Read UUID and check it's valid
            String rawUuid = ps.getStr("plugin/uuid");
            UUID uuid = UUID.parse(rawUuid);
            if(uuid == null)
            {
                core.getLogging().log("Failed to load plugin at '" + jarPath + "' - incorrect UUID '" + rawUuid + "'.", Logging.EntryType.Error);
                return PluginLoad.Failed;
            }
            // Check the UUID is not already in-use
            else if(plugins.containsKey(uuid))
            {
                core.getLogging().log("Failed to load plugin at '" + jarPath + "' - UUID (" + uuid.getHexHyphens() + ") already loaded.", Logging.EntryType.Error);
                return PluginLoad.Failed;
            }
            // Fetch the plugin class and load an instance into the runtime
            String classPath = ps.getStr("plugin/classpath");
            if(classPath == null)
            {
                core.getLogging().log("Failed to load plugin at '" + jarPath + "' - no class-path specified.", Logging.EntryType.Error);
                return PluginLoad.Failed;
            }
            Class c = jar.fetchClassType(classPath);
            Plugin p = (Plugin)c.getDeclaredConstructor(UUID.class).newInstance(uuid);
            // Add the plugin to the runtime
            plugins.put(uuid, p);
            // Inform the plugin to register to global events and templates/template-functions and that's being loaded into the runtime
            if(!p.eventHandler_registerHooks(core, this))
            {
                core.getLogging().log("Plugin '" + p.getTitle() + "' (" + uuid.getHexHyphens() + ") failed to register global event hooks!", Logging.EntryType.Error);
                unload(p);
                return PluginLoad.Failed;
            }
            else if(!p.eventHandler_registerTemplates(core, core.getTemplates()))
            {
                core.getLogging().log("Plugin '" + p.getTitle() + "' (" + uuid.getHexHyphens() + ") failed to register templates/template-functions!", Logging.EntryType.Error);
                unload(p);
                return PluginLoad.Failed;
            }
            else if(!p.eventHandler_pluginLoad(core))
            {
                core.getLogging().log("Plugin '" + p.getTitle() + "' (" + uuid.getHexHyphens() + ") failed to load!", Logging.EntryType.Error);
                unload(p);
                return PluginLoad.Failed;
            }
            core.getLogging().log("Loaded plugin '" + p.getTitle() + "' (" + uuid.getHexHyphens() + ").", Logging.EntryType.Info);
            return PluginLoad.Loaded;
        }
        catch(SettingsException ex)
        {
            core.getLogging().log("Failed to load plugin at '" + jarPath + "' - incorrect settings file.", ex, Logging.EntryType.Error);
            return PluginLoad.FailedIrrelevant;
        }
        catch(JarIOException ex)
        {
            switch(ex.getReason())
            {
                default:
                    core.getLogging().log("Failed to load potential JAR at '" + jarPath + "'.", ex, Logging.EntryType.Warning);
                    break;
                case ClassNotFound:
                    core.getLogging().log("Failed to load class of plugin at '" + jarPath + "'.", ex, Logging.EntryType.Error);
                    break;
            }
        }
        catch(InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException ex)
        {
            core.getLogging().log("Invalid class for plugin at '" + jarPath + "'.", ex, Logging.EntryType.Error);
        }
        return PluginLoad.Failed;
    }
    /**
     * Unloads a plugin from the runtime.
     * 
     * @param plugin The plugin to be removed from the runtime.
     */
    public synchronized void unload(Plugin plugin)
    {
        // -- check it's loaded in the runtime!
        // Unload a plugin from the runtime
        // remember to un-reg hooks, template funcs etc.
    }
    /**
     * Unloads all the plugins from the runtime.
     */
    public synchronized void unload()
    {
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
