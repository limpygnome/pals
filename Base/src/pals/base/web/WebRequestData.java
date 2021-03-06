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
package pals.base.web;

import java.io.IOException;
import java.util.HashMap;
import pals.base.Logging;
import pals.base.NodeCore;
import pals.base.auth.User;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;

/**
 * A wrapper for holding any instance data. transported around the node, when
 * handling a web-request. This also transports any template data, used to
 * construct and render the web-page sent to the end-user.
 * 
 * Thread-safe.
 * *****************************************************************************
 * The following template data items are used:
 * 
 * pals_page            The path of the template to use for rendering; can be
 *                      left optional for pals/page to be used by default.
 * pals_content         The template to be used for rendering content.
 * pals_title           The current title of the page.
 * pals_header          Any additional HTML to be appended to the header of the
 *                      page.
 * pals_node            The UUID of the node responsible for rendering the page.
 * pals_time            The time taken to process the request.
 * pals_institution     The name of the institution; this can be null
 *                      (optional). This is controlled by the node setting:
 *                      'templates/institution' ~ str/string
 * user                 The user object from pals.base.auth; this is not set
 *                      if the end-user is not an  authenticated user.
 * 
 * Optionally the buffer of the RemoteResponse object can be set, which will
 * avoid any template rendering and send raw bytes.
 * 
 * The following template paths are used by default:
 * pals/page    The main layout.
 * 
 * *****************************************************************************
 * In the event a request cannot be served, a plugin hook/event is raised.
 * Refer to PluginManager for the event name.
 * 
 * Refer to pals.base.TemplateManager for more information about template data.
 * 
 * @version 1.0
 */
public class WebRequestData
{
    // Fields - Constants ******************************************************
    private static final String             LOGGING_ALIAS = "PALS W.R Data";
    // Fields ******************************************************************
    private final NodeCore                  core;           // Current instance of the core.
    private final Connector                 connector;      // Database connector.
    private final RemoteRequest             request;        // Remote request data.
    private final RemoteResponse            response;       // Remote response data.
    private final HashMap<String,Object>    templateData;   // Template data for the current request.
    private DatabaseHttpSession             session;        // Session data.
    private User                            user;           // The current user for the request.
    // Methods - Constructors **************************************************
    private WebRequestData(NodeCore core, Connector connector, RemoteRequest request, RemoteResponse response)
    {
        this.core = core;
        this.connector = connector;
        this.request = request;
        this.response = response;
        this.templateData = new HashMap<>();
        this.user = null;
    }
    // Methods *****************************************************************
    /**
     * Appends a CSS file to the header.
     * 
     * @param relUrl Relative URL.
     * @since 1.0
     */
    public synchronized void appendHeaderCSS(String relUrl)
    {
        appendHeader("<link rel=\"Stylesheet\" href=\""+relUrl+"\">");
    }
    /**
     * Appends a JavaScript file to the header.
     * 
     * @param relUrl Relative URL.
     * @since 1.0
     */
    public synchronized void appendHeaderJS(String relUrl)
    {
        appendHeader("<script src=\""+relUrl+"\"></script>");
    }
    /**
     * Appends data to the header.
     * 
     * @param data The data to be appended.
     * @since 1.0
     */
    public synchronized void appendHeader(String data)
    {
        String prevData = (String)templateData.get("pals_header");
        StringBuilder sb = prevData != null ? new StringBuilder(prevData) : new StringBuilder();
        sb.append(data);
        templateData.put("pals_header", sb.toString());
    }
    // Methods - Static ********************************************************
    /**
     * Creates a new instance.
     * 
     * @param core Current instance of core.
     * @param connector Database connector.
     * @param request Request model.
     * @param response Response model.
     * @return New instance.
     * @since 1.0
     */
    public static WebRequestData create(NodeCore core, Connector connector, RemoteRequest request, RemoteResponse response)
    {
        WebRequestData data = new WebRequestData(core, connector, request, response);
        try
        {
            data.session = DatabaseHttpSession.load(connector, request.getSessionID(), request.getIpAddress());
        }
        catch(ClassNotFoundException | DatabaseException | IOException | IllegalArgumentException ex)
        {
            core.getLogging().logEx(LOGGING_ALIAS, "Could not load session data for user.", ex, Logging.EntryType.Warning);
            // Failed to load the session - give the user a new session...
            try
            {
                data.session = DatabaseHttpSession.load(connector, null, request.getIpAddress());
            }
            catch(ClassNotFoundException | DatabaseException | IOException | IllegalArgumentException ex2)
            {
                // Possibly a serious error...log and abort handling the request...
                core.getLogging().logEx(LOGGING_ALIAS, "Could not load session data for user.", ex2, Logging.EntryType.Warning);
                return null;
            }
        }
        return data;
    }
    // Methods - Accessors *****************************************************
    /**
     * Retrieves the current instance of the core.
     * 
     * @return Current instance of {@link Core}.
     * @since 1.0
     */
    public synchronized NodeCore getCore()
    {
        return core;
    }
    /**
     * Retrieves the database connector for this request.
     * 
     * @return The database connector for this request.
     * @since 1.0
     */
    public synchronized Connector getConnector()
    {
        return connector;
    }
    /**
     * Retrieves the remote request data for this request.
     * 
     * @return Request model.
     * @since 1.0
     */
    public synchronized RemoteRequest getRequestData()
    {
        return request;
    }
    /**
     * Retrieves the remote response data for this request.
     * 
     * @return Response model.
     * @since 1.0
     */
    public synchronized RemoteResponse getResponseData()
    {
        return response;
    }
    /**
     * Indicates if a template data item exists.
     * 
     * @param key The key/name of the item.
     * @return True = exists, false = does not exist.
     * @since 1.0
     */
    public synchronized boolean containsTemplateData(String key)
    {
        return templateData.containsKey(key);
    }
    /**
     * Retrieves a variable used for template rendering.
     * 
     * @param key The key of the template data.
     * @return The template data associated with the key or null.
     * @since 1.0
     */
    public synchronized Object getTemplateData(String key)
    {
        return templateData.get(key);
    }
    /**
     * Retrieves an internal map used for variables for template rendering.
     * 
     * @return The internal map.
     * @since 1.0
     */
    public synchronized HashMap<String,Object> getTemplateMap()
    {
        return templateData;
    }
    /**
     * Retrieves the session data/manager associated with the current user
     * making the request.
     * 
     * @return Session manager.
     * @since 1.0
     */
    public synchronized DatabaseHttpSession getSession()
    {
        return session;
    }
    /**
     * Retrieves the user of the current request.
     * 
     * @return A model of the current, abstract, user for the current request.
     * @since 1.0
     */
    public synchronized User getUser()
    {
        return user;
    }
    // Methods - Mutators ******************************************************
    /**
     * Sets a template variable.
     * 
     * @param key The key of the template data.
     * @param data The template data.
     * @since 1.0
     */
    public synchronized void setTemplateData(String key, Object data)
    {
        templateData.put(key, data);
    }
    /**
     * Sets the current user for the request.
     * 
     * @param user The user.
     * @since 1.0
     */
    public synchronized void setUser(User user)
    {
        this.user = user;
    }
}
