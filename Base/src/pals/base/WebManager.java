package pals.base;

import java.io.IOException;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.web.RemoteRequest;
import pals.base.web.RemoteResponse;
import pals.base.web.WebRequestData;

/**
 * Responsible for forwarding web-requests to plugins.
 * 
 * Thread-safe.
 */
public class WebManager
{
    // Fields ******************************************************************
    private NodeCore    core;           // The current instance of the node core.
    private UrlTree     urls;           // Used for finding which plugins are used when forwarding requests.
    // Methods - Constructors **************************************************
    /**
     * Creates a new web-manager.
     * 
     * @param core The current instance of the core.
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
     */
    public synchronized boolean reload()
    {
        core.getLogging().log("[WEB] Reloading all URLs.", Logging.EntryType.Info);
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
     */
    public void handleWebRequest(RemoteRequest request, RemoteResponse response)
    {
        core.getLogging().log("[WEB] New request from '" + request.getIpAddress() + "' ~ '" + request.getRelativeUrl() + "'.", Logging.EntryType.Info);
        // Create a new connection to the database
        Connector conn = core.createConnector();
        if(conn == null)
            throw new IllegalStateException("Failed to connect to the database.");
        // Create wrapper to contain data
        WebRequestData data = WebRequestData.create(core, conn, request, response);
        if(data == null)
            throw new IllegalStateException("Failed to prepare web-request, cannot continue (most likely an issue with loading session data)...");
        // Invoke webrequest start plugins
        Plugin[] plugins = core.getPlugins().getPlugins("base.web.request_start");
        Object[] args = new Object[]{data};
        for(Plugin plugin : plugins)
            plugin.eventHandler_handleHook("base.web.request_start", args);
        // Fetch plugins capable of serving the request, else fetch pagenotfound handlers
        UUID[] uuids = urls.getUUIDs(request.getRelativeUrl());
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
            // Fetch 404 handlers
            plugins = core.getPlugins().getPlugins("base.web.request_404");
            for(Plugin p : plugins)
            {
                if(p.eventHandler_handleHook("base.web.request_404", args))
                {
                    handled = true;
                    break;
                }
            }
            if(!handled)
            {
                // Set default 404 page
                data.setTemplateData("pals_content", "pals/404");
            }
        }
        // Invoke webrequest end plugins
        plugins = core.getPlugins().getPlugins("base.web.request_end");
        for(Plugin plugin : plugins)
            plugin.eventHandler_handleHook("base.web.request_end", args);
        // Render template and update response data
        // -- Unless the buffer has been set manually
        if((response.getBuffer() == null || response.getBuffer().length == 0) && data.getTemplateData("pals_content") != null)
        {
            String dd = core.getTemplates().render(data, "pals/page");
            response.setBuffer(dd);
        }
        // Update session ID
        response.setSessionID(data.getSession().getIdBase64());
        // Persist session data
        try
        {
            data.getSession().persist(conn);
        }
        catch(DatabaseException | IOException ex)
        {
            core.getLogging().log("[WEB] Failed to persist session data of user.", ex, Logging.EntryType.Warning);
        }
        // Dispose resources
        try
        {
            conn.disconnect();
        }
        catch(DatabaseException ex)
        {
            core.getLogging().log("[WEB] Exception thrown disposing web connector.", ex, Logging.EntryType.Warning);
        }
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The URL tree used to store the plugins used to handle paths/URLs.
     */
    public synchronized UrlTree getUrlTree()
    {
        return urls;
    }
    // Methods - Mutators ******************************************************
    /**
     * Used to register paths for forwarding web-requests to a plugin.
     * 
     * @param plugin The plugin of where requests should be dispatched for the
     * specified paths.
     * @param paths The paths to be associated with the specified plugin.
     * @return True = added successfully, false = an error occurred (most likely
     * a conflicting plugin or possibly a malformed path).
     */
    public synchronized boolean registerUrls(Plugin plugin, String[] paths)
    {
        if(plugin == null)
            return false;
        // Register each URL
        UrlTree.RegisterStatus rs;
        for(String path : paths)
        {
            if((rs = urls.add(plugin, path)) != UrlTree.RegisterStatus.Success)
            {
                core.getLogging().log("[WEB] Failed to add path '" + path + "' for plugin '" + plugin.getUUID().getHexHyphens() + "' - '" + rs + "'!", Logging.EntryType.Warning);
                return false;
            }
        }
        return true;
    }
    /**
     * Removes all paths associated with a plugin.
     * 
     * @param plugin The plugin associated with the paths to be removed.
     */
    public synchronized void unregisterUrls(Plugin plugin)
    {
        if(plugin != null)
            urls.remove(plugin);
    }
}
