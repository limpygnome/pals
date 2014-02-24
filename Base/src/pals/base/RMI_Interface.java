
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
     * Invokes plugins subscribed to a global hook until it's handled by a
     * plugin.
     * 
     * @param event The name of the global event.
     * @param data The data to be passed to plugins; must be serializable.
     * @return True = a plugin handled the event, false = no plugins,
     * subscribed, have handled the event.
     * @throws RemoteException Thrown if an issue occurs with RMI.
     */
    public boolean invokeGlobalHook(String event, Object[] data) throws RemoteException;;
    /**
     * Invokes all of the plugins subscribed to a global hook.
     * 
     * @param event The name of the global event.
     * @param data The data to be passed to plugins; must be serializable.
     * @throws RemoteException Thrown if an issue occurs with RMI.
     */
    public void invokeGlobalHookAll(String event, Object[] data) throws RemoteException;;
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
