package pals.plugins;

import pals.base.NodeCore;
import pals.base.UUID;
import pals.base.WebManager;
import pals.base.web.WebRequestData;

/**
 * An example plugin.
 */
public class Example extends pals.base.Plugin
{
    public Example(UUID uuid)
    {
        super(uuid);
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
        // Unregister URLs
        core.getWebManager().unregisterUrls(this);
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
        data.getResponseData().setBuffer("<!--if:test-->case a<!--else:test-->case b<!--endif:test-->");
        return true;
    }
    @Override
    public String getTitle()
    {
        return "Example Plugin";
    }
}
