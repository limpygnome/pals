package pals.base;

import pals.base.web.WebRequestData;

/**
 * The base-class of all plugins.
 */
public class Plugin
{    
    // Enums *******************************************************************
    public enum State
    {
        NotInstalled,
        Disabled,
        Enabled
    }
    public enum ActionType
    {
        Install,
        Enable,
        Disable,
        Uninstall
    }
    // Fields ******************************************************************
    private final UUID          uuid;           // The unique identifier of this plugin.
    // Methods - Constructors **************************************************
    public Plugin(UUID uuid)
    {
        this.uuid = uuid;
    }
    // Methods - Mandatory *****************************************************
    /**
     * Invoked when the plugin is loaded into the runtime.
     * 
     * @param core The current instance of the core.
     * @return True if successful, false if failed.
     */
    public boolean eventHandler_pluginLoad(NodeCore core)
    {
        return false;
    }
    /**
     * Invoked to install the plugin.
     * 
     * @param core The current instance of the core.
     * @return True if successful, false if failed.
     */
    public boolean eventHandler_pluginInstall(NodeCore core)
    {
        return false;
    }
    /**
     * Invoked to uninstall the plugin.
     * 
     * @param core The current instance of the core.
     * @return True if successful, false if failed.
     */
    public boolean eventHandler_pluginUninstall(NodeCore core)
    {
        return false;
    }
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
    public boolean eventHandler_templatesLoad(NodeCore core, TemplateManager manager)
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
     * Invoked to handle an action applied to a plugin (not its self). This is
     * useful for aborting the install/uninstall of other plugins.
     * 
     * @param core The current instance of the core.
     * @param plugin The plugin in question.
     * @param action The action being applied to the plugin.
     * @return True if successfully handled, false if to abort the action (if
     * possible).
     */
    public boolean eventHandler_pluginAction(NodeCore core, Plugin plugin, ActionType action)
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
     * Invoked to handle the start of a web-request, before it's handled by a
     * a plugin.
     * 
     * @param data Data wrapper for the web-request.
     */
    public void eventHandler_webRequestStart(WebRequestData data)
    {
    }
    /**
     * Invoked to handle the end of a web-request, after the request has been
     * handled by a plugin.
     * 
     * @param data Data wrapper for the web-request.
     */
    public void eventHandler_webRequestStop(WebRequestData data)
    {
    }
    
    
    // Ideas to be implemented later.
    public boolean eventHandler_webRequest_renderQuestionType()
    {
        return false;
    }
    public boolean eventHandler_webRequest_renderCriteriaType()
    {
        return false;
    }
    public boolean eventHandler_webRequest_renderQuestion()
    {
        return false;
    }
    
    // Methods - Accessors *****************************************************
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
     * @return Indicates if the plugin is a system plugin; this means the
     * plugin cannot be uninstalled and has elevated privileges.
     */
    public boolean isSystemPlugin()
    {
        return false;
    }
}
