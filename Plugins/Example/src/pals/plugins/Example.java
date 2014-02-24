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
    Version:    1.0
    Authors:    Marcus Craske           <limpygnome@gmail.com>
    ----------------------------------------------------------------------------
*/
package pals.plugins;

import pals.base.NodeCore;
import pals.base.PluginManager;
import pals.base.Settings;
import pals.base.TemplateManager;
import pals.base.UUID;
import pals.base.Version;
import pals.base.WebManager;
import pals.base.database.Connector;
import pals.base.utils.JarIO;
import pals.base.web.MultipartUrlParser;
import pals.base.web.WebRequestData;

/**
 * An example plugin.
 */
public class Example extends pals.base.Plugin
{
    public Example(NodeCore core, UUID uuid, JarIO jario, Version version, Settings settings, String jarPath)
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
        return true;
    }
    @Override
    public boolean eventHandler_registerHooks(NodeCore core, PluginManager plugins)
    {
        // Hook 404 event
        plugins.globalHookRegister(this, "base.web.request_404");
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
                        data.setTemplateData("pals_title", "Example 404 Page");
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
        core.getWebManager().urlsUnregister(this);
        // Unregister templates
        core.getTemplates().remove(this);
        // Unregister hooks
        core.getPlugins().globalHookUnregister(this);
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
        return web.urlsRegister(this, new String[]
        {
            "admin",
            "home",
            "hello_world"
        });
    }
    @Override
    public boolean eventHandler_webRequest(WebRequestData data)
    {
        MultipartUrlParser mup = new MultipartUrlParser(data);
        switch(mup.getPart(0))
        {
            case "hello_world":
                data.setTemplateData("pals_title", "Hello World!");
                data.setTemplateData("pals_content", "test");
                break;
            case "home":
                data.setTemplateData("pals_title", "Welcome!");
                data.setTemplateData("pals_content", "home");
                break;
            case "admin":
                if(data.getUser() == null || !data.getUser().getGroup().isAdmin())
                    return false;
                data.setTemplateData("pals_title", "Admin");
                data.setTemplateData("pals_content", "admin");
                break;
            default:
                return false;
        }
        return true;
    }
    @Override
    public String getTitle()
    {
        return "Example Plugin";
    }
}
