package pals.plugins;

import pals.base.NodeCore;
import pals.base.PluginManager;
import pals.base.Settings;
import pals.base.TemplateManager;
import pals.base.UUID;
import pals.base.WebManager;
import pals.base.web.WebRequestData;

/**
 * An example plugin.
 */
public class Example extends pals.base.Plugin
{
    public Example(NodeCore core, UUID uuid, Settings settings, String jarPath)
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
    public boolean eventHandler_registerHooks(NodeCore core, PluginManager plugins)
    {
        // Hook 404 event
        plugins.registerGlobalEvent(this, "base.web.request_404");
        return true;
    }

    @Override
    public boolean eventHandler_handleHook(String event, Object[] args)
    {
        switch(event)
        {
            case "base.web.request_404":
                // Handles a 404 page when hello_404 is requested and a 404 event occurs
                if(args.length == 1 && args[0] instanceof WebRequestData)
                {
                    WebRequestData data = (WebRequestData)args[0];
                    if(data.getRequestData().getRelativeUrl().equals("hello_404"))
                    {
                        data.setTemplateData("pals_content", "example_404");
                        return true;
                    }
                }
                break;
        }
        return false;
    }
    @Override
    public void eventHandler_pluginUnload(NodeCore core)
    {
        // Unregister URLs
        core.getWebManager().unregisterUrls(this);
        // Unregister templates
        core.getTemplates().remove(this);
    }
    @Override
    public boolean eventHandler_registerTemplates(NodeCore core, TemplateManager manager)
    {
        // Load templates from our jar
        if(!manager.load(this, "templates"))
            return false;
        // Register example function
        if(!manager.registerFunction("hello", new ExampleTemplateFunction(this)))
            return false;
        return true;
    }
    @Override
    public boolean eventHandler_registerUrls(NodeCore core, WebManager web)
    {
        return web.registerUrls(this, new String[]
        {
            "hello_world"
        });
    }
    @Override
    public boolean eventHandler_webRequest(WebRequestData data)
    {
        data.setTemplateData("pals_content", "test");
        return true;
    }
    @Override
    public String getTitle()
    {
        return "Example Plugin";
    }
}
