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
    Version:    1.0
    Authors:    Marcus Craske           <limpygnome@gmail.com>
    ----------------------------------------------------------------------------
*/
package pals.rmi;

import java.rmi.*;
import pals.base.UUID;
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
