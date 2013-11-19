package pals.base.web;

import java.util.HashMap;
import pals.base.NodeCore;
import pals.base.database.Connector;

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
    // Methods - Constructors **************************************************
    public WebRequestData(NodeCore core, Connector connector, RemoteRequest request, RemoteResponse response)
    {
        this.core = core;
        this.connector = connector;
        this.request = request;
        this.response = response;
        this.templateData = new HashMap<>();
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The current instance of the core.
     */
    public NodeCore getCore()
    {
        return core;
    }
    /**
     * @return The database connector for this request.
     */
    public Connector getConnector()
    {
        return connector;
    }
    /**
     * @return The remote request data for this request.
     */
    public RemoteRequest getRequestData()
    {
        return request;
    }
    /**
     * @return The remote response data for this request.
     */
    public RemoteResponse getResponseData()
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
