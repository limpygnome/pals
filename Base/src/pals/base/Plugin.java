package pals.base;

import pals.base.database.Connector;
import pals.base.utils.JarIO;
import pals.base.web.WebRequestData;

/**
 * The base class for all plugins.
 * 
 * Notes:
 * - If a plugin is not installed, it will not be allowed near the runtime.
 * - An uninstalled plugin will be rejected when being loaded into the runtime.
 */
public abstract class Plugin
{
    // Fields ******************************************************************
    private JarIO               jario;          // Used for interfacing with the plugin's jar.
    private final UUID          uuid;           // The unique identifier of this plugin.
    private final NodeCore      core;           // The current instance of the core.
    protected final Settings    settings;       // The plugin's settings (read-only).
    private final String        jarLocation;    // The location of where the plugin was loaded.
    private final Version       version;        // The version of the plugin.
    // Methods - Constructors **************************************************
    public Plugin(NodeCore core, UUID uuid, JarIO jario, Version version, Settings settings, String jarLocation)
    {
        this.core = core;
        this.uuid = uuid;
        this.jario = jario;
        this.version = version;
        this.settings = settings;
        this.jarLocation = jarLocation;
    }
    // Methods - Mandatory *****************************************************
    /**
     * Invoked when the plugin is loaded into the runtime.
     * 
     * @param core The current instance of the core.
     * @return True if successful, false if failed.
     */
    public abstract boolean eventHandler_pluginLoad(NodeCore core);
    /**
     * Invoked to install the plugin.
     * 
     * @param core The current instance of the core.
     * @param conn Database connector; this must not be disconnected.
     * @return True if successful, false if failed.
     */
    public abstract boolean eventHandler_pluginInstall(NodeCore core, Connector conn);
    /**
     * Invoked to uninstall the plugin.
     * 
     * @param core The current instance of the core.
     * @param conn Database connector; this must not be disconnected.
     * @return True if successful, false if failed.
     */
    public abstract boolean eventHandler_pluginUninstall(NodeCore core, Connector conn);
    // Methods - Optional ******************************************************
    /**
     * Invoked before the plugin is unloaded from the runtime.
     * 
     * @param core The current instance of the core.
     */
    public void eventHandler_pluginUnload(NodeCore core)
    {
    }
    /**
     * Invoked when a plugin should register any template(s) or template
     * function(s).
     * 
     * @param core The current instance of the core.
     * @param manager The template manager.
     * @return True = success, false = failed.
     */
    public boolean eventHandler_registerTemplates(NodeCore core, TemplateManager manager)
    {
        return true;
    }
    /**
     * Invoked when a plugin should register for global events.
     * 
     * @param core The current instance of the core.
     * @param plugins The plugin manager.
     * @return True if successfully registered, false if failed.
     */
    public boolean eventHandler_registerHooks(NodeCore core, PluginManager plugins)
    {
        return true;
    }
    /**
     * Invoked when a plugin should register any URLs for forwarding
     * web-requests back to the plugin for specific paths.
     * 
     * @param core The current instance of the core.
     * @param web The web manager.
     * @return True if successfully registered, false if failed.
     */
    public boolean eventHandler_registerUrls(NodeCore core, WebManager web)
    {
        return true;
    }
    /**
     * Invoked to handle a web-request mapped to this plugin.
     * 
     * @param data Data wrapper for the web-request.
     * @return True if handled, false if not handled.
     */
    public boolean eventHandler_webRequest(WebRequestData data)
    {
        return false;
    }
    /**
     * Used to handle an event.
     * 
     * @param event The name of the event.
     * @param args The arguments of the event; these are up to the invoking
     * plugin. Therefore these should be checked.
     * @return True = handled, false = not handled.
     */
    public boolean eventHandler_handleHook(String event, Object[] args)
    {
        return false;
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The instance of the core of where this plugin is operating.
     */
    public NodeCore getCore()
    {
        return core;
    }
    /**
     * @return The unique identifier for this plugin.
     */
    public UUID getUUID()
    {
        return uuid;
    }
    /**
     * @return The title of this plugin, used for debugging and human
     * identification purposes.
     */
    public String getTitle()
    {
        return "Untitled Plugin";
    }
    /**
     * @return The location of the JAR file used to load this plugin; this
     * is not required and may even change during runtime. Therefore this can
     * return null or an invalid path.
     */
    public String getJarLocation()
    {
        return jarLocation;
    }
    /**
     * @return Indicates if the plugin is a system plugin; this means the
     * plugin cannot be uninstalled and has elevated privileges.
     */
    public boolean isSystemPlugin()
    {
        return false;
    }
    /**
     * @return The plugin's settings.
     */
    public Settings getSettings()
    {
        return settings;
    }
    /**
     * @return The underlying class handling I/O for this jar; this may be
     * null if disposed.
     */
    public JarIO getJarIO()
    {
        return jario;
    }
    /**
     * @return The version of the plugin.
     */
    public Version getVersion()
    {
        return version;
    }
}
