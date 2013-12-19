package pals.base.web;

import java.io.IOException;
import java.util.HashMap;
import pals.base.Logging;
import pals.base.NodeCore;
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
 */
public class WebRequestData
{
    // Fields ******************************************************************
    private final NodeCore                  core;           // Current instance of the core.
    private final Connector                 connector;      // Database connector.
    private final RemoteRequest             request;        // Remote request data.
    private final RemoteResponse            response;       // Remote response data.
    private final HashMap<String,Object>    templateData;   // Template data for the current request.
    private DatabaseHttpSession             session;        // Session data.
    // Methods - Constructors **************************************************
    private WebRequestData(NodeCore core, Connector connector, RemoteRequest request, RemoteResponse response)
    {
        this.core = core;
        this.connector = connector;
        this.request = request;
        this.response = response;
        this.templateData = new HashMap<>();
    }
    // Methods - Static ********************************************************
    public static WebRequestData create(NodeCore core, Connector connector, RemoteRequest request, RemoteResponse response)
    {
        WebRequestData data = new WebRequestData(core, connector, request, response);
        try
        {
            data.session = DatabaseHttpSession.load(connector, request.getSessionID(), request.getIpAddress());
        }
        catch(ClassNotFoundException | DatabaseException | IOException | IllegalArgumentException ex)
        {
            core.getLogging().log("Could not load session data for user.", ex, Logging.EntryType.Warning);
            // Failed to load the session - give the user a new session...
            try
            {
                data.session = DatabaseHttpSession.load(connector, null, request.getIpAddress());
            }
            catch(ClassNotFoundException | DatabaseException | IOException | IllegalArgumentException ex2)
            {
                // Possibly a serious error...log and abort handling the request...
                core.getLogging().log("Could not load session data for user.", ex2, Logging.EntryType.Warning);
                return null;
            }
        }
        return data;
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The current instance of the core.
     */
    public synchronized NodeCore getCore()
    {
        return core;
    }
    /**
     * @return The database connector for this request.
     */
    public synchronized Connector getConnector()
    {
        return connector;
    }
    /**
     * @return The remote request data for this request.
     */
    public synchronized RemoteRequest getRequestData()
    {
        return request;
    }
    /**
     * @return The remote response data for this request.
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
     */
    public synchronized boolean containsTemplateData(String key)
    {
        return templateData.containsKey(key);
    }
    /**
     * @param key The key of the template data.
     * @return The template data associated with the key or null.
     */
    public synchronized Object getTemplateData(String key)
    {
        return templateData.get(key);
    }
    
    public synchronized HashMap<String,Object> getTemplateMap()
    {
        return templateData;
    }
    /**
     * @return The session data/manager associated with the current user making
     * the request.
     */
    public synchronized DatabaseHttpSession getSession()
    {
        return session;
    }
    // Methods - Mutators ******************************************************
    /**
     * @param key The key of the template data.
     * @param data The template data.
     */
    public synchronized void setTemplateData(String key, Object data)
    {
        templateData.put(key, data);
    }
}
