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
 * Refer to pals.base.TemplateManager for more information about template data.
 * 
 * Thread-safe.
 */
public class WebRequestData
{
    // Fields ******************************************************************
    private final NodeCore                  core;           // Current instance of the core.
    private final Connector                 connector;      // Database connector.
    private final RemoteRequest             request;        // Remote request data.
    private final RemoteResponse            response;       // Remote response data.
    private final HashMap<String,String>    templateData;   // Template data for the current request.
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
     * @param key The key of the template data.
     * @return The template data associated with the key or null.
     */
    public synchronized String getTemplateData(String key)
    {
        return templateData.get(key);
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
    public synchronized void setTemplateData(String key, String data)
    {
        templateData.put(key, data);
    }
}
