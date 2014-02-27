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

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import pals.base.Logging;
import pals.base.NodeCore;
import pals.base.Plugin;
import pals.base.PluginManager;
import pals.base.rmi.RMI_Interface;
import pals.base.Settings;
import pals.base.TemplateManager;
import pals.base.UUID;
import pals.base.Version;
import pals.base.WebManager;
import pals.base.auth.User;
import pals.base.database.Connector;
import pals.base.utils.JarIO;
import pals.base.web.MultipartUrlParser;
import pals.base.web.RemoteRequest;
import pals.base.web.WebRequestData;
import pals.base.web.security.CSRF;
import pals.plugins.nodesactive.ModelNode;
import pals.base.rmi.RMI;
import pals.base.rmi.RMI_Host;

/**
 * A very simple plugin to update the active-time, periodically, of a node in
 * the database.
 */
public class NodeActive extends Plugin
{
    // Fields ******************************************************************
    private NodeActiveThread nat;
    // Constants ***************************************************************
    public static final String LOGGING_ALIAS = "Node Active";
    // Methods - Constructor ***************************************************
    public NodeActive(NodeCore core, UUID uuid, JarIO jario, Version version, Settings settings, String jarPath)
    {
        super(core, uuid, jario, version, settings, jarPath);
        nat = new NodeActiveThread(this);
    }
    @Override
    public boolean eventHandler_pluginLoad(NodeCore core)
    {
        // Setup thread to update the database
        core.getLogging().log(LOGGING_ALIAS, "Starting status thread.", Logging.EntryType.Info);
        nat.start();
        return true;
    }
    @Override
    public void eventHandler_pluginUnload(NodeCore core)
    {
        // Dispose thread
        core.getLogging().log(LOGGING_ALIAS, "Stopping status thread...", Logging.EntryType.Info);
        nat.extended_stop();
        try
        {
            nat.join();
        }
        catch(InterruptedException ex){}
        core.getLogging().log(LOGGING_ALIAS, "Stopped status thread.", Logging.EntryType.Info);
        // Dispose URLs
        core.getWebManager().urlsUnregister(this);
        // Dispose templates
        core.getTemplates().remove(this);
    }
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
            "admin/nodes"
        }))
            return false;
        return true;
    }
    @Override
    public boolean eventHandler_webRequest(WebRequestData data)
    {
        MultipartUrlParser mup = new MultipartUrlParser(data);
        switch(mup.getPart(0))
        {
            case "admin":
                // Check user is authenticated
                User usr = data.getUser();
                if(usr == null || !usr.getGroup().isAdminSystem())
                    return false;
                // Handle admin pages
                switch(mup.getPart(1))
                {
                    case "nodes":
                        return pageAdmin_nodes(data);
                }
                break;
        }
        return false;
    }
    public boolean pageAdmin_nodes(WebRequestData data)
    {
        // Process actions
        RemoteRequest req = data.getRequestData();
        String uuid = req.getField("uuid");
        String all = req.getField("all");
        String action = req.getField("action");
        if(uuid != null && action != null)
        {
            UUID nodeUUID = UUID.parse(uuid);
            ModelNode node;
            if(!CSRF.isSecure(data))
                data.setTemplateData("error", "Invalid request; please try again.");
            else if(nodeUUID == null || (node = ModelNode.load(data.getConnector(), nodeUUID)) == null)
                return false;
            else
            {
                switch(action)
                {
                    case "restart":
                    case "shutdown":
                        if(node.getRmiIP() == null || node.getRmiPort() == null)
                            data.setTemplateData("error", "Node has no RMI information associated; host cannot be contacted.");
                        else
                        {
                            try
                            {
                                RMI_Interface ri = data.getCore().getRMI().fetchRMIConnection(node.getRmiIP(), node.getRmiPort());
                                if(action.equals("shutdown"))
                                    ri.shutdown();
                                else
                                    ri.restart();
                                data.setTemplateData("success", "Node has been instructed to shutdown.");
                            }
                            catch(NotBoundException | RemoteException ex)
                            {
                                data.getCore().getLogging().logEx("test", ex, Logging.EntryType.Error);
                                data.setTemplateData("warning", "Could not contact node; possibly already shutdown/restarting.");
                            }
                        }
                        break;
                    default:
                        return false;
                }
            }
        }
        else if(all != null && all.equals("1") && action != null)
        {
            if(!CSRF.isSecure(data))
                data.setTemplateData("error", "Invalid request; please try again.");
            else if(action.equals("shutdown") || action.equals("restart"))
            {
                RMI rmi = data.getCore().getRMI();
                RMI_Interface ri;
                for(RMI_Host h : rmi.getNodes())
                {
                    try
                    {
                        ri = rmi.fetchRMIConnection(h.getHost(), h.getPort());
                        switch(action)
                        {
                            case "shutdown":
                                ri.shutdown();
                                break;
                            case "restart":
                                ri.restart();
                                break;
                        }
                    }
                    catch(NotBoundException | RemoteException ex)
                    {
                    }
                }
            }
        }
        // Fetch nodes
        ModelNode[] nodes = ModelNode.load(data.getConnector());
        // Setup page
        data.setTemplateData("pals_title", "Nodes");
        data.setTemplateData("pals_content", "nodes_active/page_admin_nodes");
        data.setTemplateData("nodes", nodes);
        data.setTemplateData("csrf", CSRF.set(data));
        return true;
    }
    @Override
    public String getTitle()
    {
        return "PALS: Node Active";
    }
}
