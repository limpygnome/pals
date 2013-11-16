
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
     * @param request The remote web request data.
     * @param response The remote web response data.
     * @throws RemoteException Thrown if an issue occurs with RMI.
     */
    public void handleWebRequest(RemoteRequest request, RemoteResponse response) throws RemoteException;
}
