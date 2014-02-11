
package pals.base;

import java.rmi.*;
import pals.base.web.RemoteRequest;
import pals.base.web.RemoteResponse;

/**
 * The interface used for RMI communication.
 */
public interface RMI_Interface extends Remote
{
    /**
     * Used to remotely handle a web-request.
     * 
     * @param request The remote web request data.
     * @return Response data for the request.
     * @throws RemoteException Thrown if an issue occurs with RMI.
     */
    public RemoteResponse handleWebRequest(RemoteRequest request) throws RemoteException;
    /**
     * Unloads a plugin from the runtime.
     * 
     * @param plugin The identifier of the plugin to unload.
     * @throws RemoteException Thrown if an issue occurs with RMI.
     */
    public void pluginUnload(UUID plugin) throws RemoteException;
    /**
     * Restarts the node.
     * 
     * @throws RemoteException Thrown if an issue occurs with RMI.
     */
    public void restart() throws RemoteException;
    /**
     * Informs this node to shutdown; this will not shutdown the physical host.
     * 
     * @throws RemoteException Thrown if an issue occurs with RMI.
     */
    public void shutdown() throws RemoteException;
}
