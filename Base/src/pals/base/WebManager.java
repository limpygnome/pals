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

import java.io.IOException;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.web.RemoteRequest;
import pals.base.web.RemoteResponse;
import pals.base.web.WebRequestData;

/**
 * Responsible for forwarding web-requests to plugins and management of
 * URLs assigned to plugins.
 * 
 * Relative URLs are assigned as belonging to a plugin, with different folders
 * able to go to different plugins. If a plugin is unable to handle a request,
 * the next root folder of the URL is sent the request. Thus this means
 * regular expression matching is not needed; URLs are also stored as a
 * tree for efficiency, rather than iterating a list of regular expressions.
 * Therefore only one plugin may own a folder, however a sub-directory or/and
 * root can be owned by different plugins.
 * 
 * Thread-safe.
 * 
 * @version 1.0
 */
public class WebManager
{
    // Fields - Constants ******************************************************
    private static final String LOGGING_ALIAS = "PALS Web Man.";
    private static final String DEFAULT_URL = "home";
    // Fields ******************************************************************
    private NodeCore    core;           // The current instance of the node core.
    private UrlTree     urls;           // Used for finding which plugins are used when forwarding requests.
    // Methods - Constructors **************************************************
    /**
     * Creates a new web-manager.
     * 
     * @param core The current instance of the core.
     * @since 1.0
     */
    protected WebManager(NodeCore core)
    {
        this.core = core;
        this.urls = new UrlTree();
    }
    // Methods *****************************************************************
    /**
     * Reloads all of the URL hooks.
     * 
     * @return True = successful, false = failed.
     * @since 1.0
     */
    public synchronized boolean reload()
    {
        core.getLogging().log(LOGGING_ALIAS, "Reloading all URLs.", Logging.EntryType.Info);
        // Clear existing tree
        urls.reset();
        // Invoke all the plugins to re-register their URLs
        for(Plugin p : core.getPlugins().getPlugins())
        {
            if(!p.eventHandler_registerUrls(core, this))
            {
                core.getLogging().log("[WEB] Failed to register URLs for plugin [" + p.getUUID().getHexHyphens() + "]!", null, Logging.EntryType.Error);
                return false;
            }
        }
        return true;
    }
    /**
     * Handles a web-request to the system.
     * 
     * @param request The request data.
     * @param response The response data.
     * @since 1.0
     */
    public void handleWebRequest(RemoteRequest request, RemoteResponse response)
    {
        long timeStart = System.currentTimeMillis();
        core.getLogging().log(LOGGING_ALIAS, "New request from '" + request.getIpAddress() + "' ~ '" + request.getRelativeUrl() + "'.", Logging.EntryType.Info);
        // Create a new connection to the database
        Connector conn = core.createConnector();
        if(conn == null)
            throw new IllegalStateException("Failed to connect to the database.");
        // Create wrapper to contain data
        WebRequestData data = WebRequestData.create(core, conn, request, response);
        if(data == null)
            throw new IllegalStateException("Failed to prepare web-request, cannot continue (most likely an issue with loading session data)...");
        // Invoke webrequest start plugins
        Object[] args = new Object[]{data};
        core.getPlugins().globalHookInvokeAll("base.web.request_start", args);
        try
        {
            // Fetch plugins capable of serving the request, else fetch pagenotfound handlers
            String relUrl = request.getRelativeUrl();
            if(relUrl.length() == 0)
            {
                data.getRequestData().setRelativeUrl(relUrl = DEFAULT_URL);
            }
            UUID[] uuids = urls.getUUIDs(relUrl.length() > 0 ? relUrl : DEFAULT_URL);
            Plugin ph;
            boolean handled = false;
            for(UUID uuid : uuids)
            {
                ph = core.getPlugins().getPlugin(uuid);
                if(ph != null && ph.eventHandler_webRequest(data))
                {
                    handled = true;
                    break;
                }
            }
            if(!handled)
            {
                // Set response code to 404 - page not found
                response.setResponseCode(404);
                // Attempt to get a plugin to handle the 404 event
                if(!core.getPlugins().globalHookInvoke("base.web.request_404", args))
                {
                    // No plugin handled the event - set the default 404 page
                    data.setTemplateData("pals_content", "pals/404");
                }
            }
        }
        catch(Throwable ex)
        {
            core.getLogging().logEx(LOGGING_ALIAS, "Failed to serve web-request.", ex, Logging.EntryType.Warning);
        }
        // Invoke webrequest end plugins
        core.getPlugins().globalHookInvokeAll("base.web.request_end", args);
        // Setup node and time variables
        data.setTemplateData("pals_node", core.getNodeUUID().getHexHyphens());
        data.setTemplateData("pals_time", System.currentTimeMillis()-timeStart);
        if(!data.containsTemplateData("pals_institution"))
        {
            String s = core.getSettings().getStr("templates/institution");
            data.setTemplateData("pals_institution", s);
        }
        if(data.getUser() != null)
            data.setTemplateData("user", data.getUser());
        data.setTemplateData("data", data);
        // Render template and update response data
        // -- Unless the buffer has been set manually
        if(response.getBuffer() == null || response.getBuffer().length == 0)
        {
            String template = (String)data.getTemplateData("pals_page");
            String dd = core.getTemplates().render(data,  template != null ? template : "pals/page");
            response.setBuffer(dd);
        }
        // Update session data in response
        response.setSessionID(data.getSession().getIdBase64());
        response.setSessionPrivate(data.getSession().isPrivate());
        // Persist session data
        try
        {
            data.getSession().persist(conn);
        }
        catch(DatabaseException | IOException ex)
        {
            core.getLogging().logEx(LOGGING_ALIAS, "Failed to persist session data of user.", ex, Logging.EntryType.Warning);
        }
        // Dispose resources
        conn.disconnect();
    }
    // Methods - Accessors *****************************************************
    /**
     * Fetches the underlying URL-tree data-structure for storing URLs.
     * 
     * @return The URL tree used to store the plugins used to handle paths/URLs.
     * @since 1.0
     */
    public synchronized UrlTree getUrlTree()
    {
        return urls;
    }
    // Methods - Mutators ******************************************************
    /**
     * Used to register paths for forwarding web-requests to a {@link Plugin}.
     * 
     * @param plugin The plugin of where requests should be dispatched for the
     * specified paths.
     * @param paths The paths to be associated with the specified plugin.
     * @return True = added successfully, false = an error occurred (most likely
     * a conflicting plugin or possibly a malformed path).
     * @since 1.0
     */
    public synchronized boolean urlsRegister(Plugin plugin, String[] paths)
    {
        if(plugin == null)
            return false;
        // Register each URL
        UrlTree.RegisterStatus rs;
        for(String path : paths)
        {
            if((rs = urls.add(plugin, path)) != UrlTree.RegisterStatus.Success)
            {
                core.getLogging().log(LOGGING_ALIAS, "Failed to add path '"+path+"' for plugin '" + plugin.getUUID().getHexHyphens() + "' - '" + rs + "'!", Logging.EntryType.Warning);
                return false;
            }
        }
        return true;
    }
    /**
     * Removes all paths associated with a {@link Plugin}.
     * 
     * @param plugin The plugin associated with the paths to be removed.
     * @since 1.0
     */
    public synchronized void urlsUnregister(Plugin plugin)
    {
        if(plugin != null)
            urls.remove(plugin);
    }
}
