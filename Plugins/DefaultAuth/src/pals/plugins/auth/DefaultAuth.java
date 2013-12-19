package pals.plugins.auth;

import pals.base.NodeCore;
import pals.base.Plugin;
import pals.base.Settings;
import pals.base.TemplateManager;
import pals.base.UUID;
import pals.base.WebManager;
import pals.base.web.WebRequestData;

/**
 * Default authentication handler for PALS.
 */
public class DefaultAuth extends Plugin
{
    // Methods - Constructors **************************************************
    public DefaultAuth(NodeCore core, UUID uuid, Settings settings, String jarPath)
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
        return true;
    }
    @Override
    public void eventHandler_pluginUnload(NodeCore core)
    {
        // Unregister templates
        core.getTemplates().remove(this);
        // Unregister URLs
        core.getWebManager().urlsUnregister(this);
    }
    @Override
    public boolean eventHandler_registerTemplates(NodeCore core, TemplateManager manager)
    {
        manager.load(this, "templates");
        return true;
    }
    @Override
    public boolean eventHandler_registerUrls(NodeCore core, WebManager web)
    {
        web.urlsRegister(this, new String[]
        {
            "account/settings",
            "account/register",
            "account/login",
            "account/logout"
        });
        return true;
    }
    @Override
    public boolean eventHandler_webRequest(WebRequestData data)
    {
        switch(data.getRequestData().getRelativeUrl())
        {
            case "account/settings":
                return pageAccountSettings(data);
            case "account/register":
                return pageAccountRegister(data);
            case "account/login":
                return pageAccountLogin(data);
            case "account/logout":
                return pageAccountLogout(data);
            default:
                return false;
        }
    }
    // Methods - Pages *********************************************************
    public boolean pageAccountSettings(WebRequestData data)
    {
        // Check form data
        
        // Setup the page
        
        return true;
    }
    public boolean pageAccountRegister(WebRequestData data)
    {
        // Check form data
        
        // Setup the page
        return true;
    }
    public boolean pageAccountLogin(WebRequestData data)
    {
        // Check form data
        
        // Setup the page
        return true;
    }
    public boolean pageAccountLogout(WebRequestData data)
    {
        // Destroy session
        
        // Setup confirmation page
        
        return true;
    }
}
