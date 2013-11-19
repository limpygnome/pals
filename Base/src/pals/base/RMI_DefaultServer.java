package pals.base;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.web.RemoteRequest;
import pals.base.web.RemoteResponse;
import pals.base.web.WebRequestData;

/**
 * The default implementation of the RMI server for PALS.
 */
public class RMI_DefaultServer extends UnicastRemoteObject  implements RMI_Interface
{
    public RMI_DefaultServer() throws RemoteException
    {
        super();
    }
    @Override
    public void handleWebRequest(RemoteRequest request, RemoteResponse response) throws RemoteException
    {
        // Get the current instance of the core
        NodeCore core = NodeCore.getInstance();
        // Create a new connection to the database
        Connector conn = core.createConnector();
        // Create wrapper to contain data
        WebRequestData data = new WebRequestData(core, conn, request, response);
        // Invoke webrequest start plugins
        
        // Fetch plugins capable of serving the request, else fetch pagenotfound handlers
        
        // Invoke webrequest end plugins
        
        // Render template and update response data
        
        // Dispose resources
        try
        {
            conn.disconnect();
        }
        catch(DatabaseException ex)
        {
            core.getLogging().log("Exception thrown disposing web connector.", ex, Logging.EntryType.Warning);
        }
    }
}
