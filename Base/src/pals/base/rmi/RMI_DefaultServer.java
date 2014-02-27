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
package pals.base.rmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import pals.base.NodeCore;
import pals.base.Plugin;
import pals.base.UUID;
import pals.base.web.RemoteRequest;
import pals.base.web.RemoteResponse;

/**
 * The default implementation of the RMI server for PALS.
 */
public class RMI_DefaultServer extends UnicastRemoteObject  implements RMI_Interface
{
    // Fields ******************************************************************
    private NodeCore    core;       // The current instance of the core.
    // Methods - Constructors **************************************************
    public RMI_DefaultServer(NodeCore core) throws RemoteException
    {
        super();
        this.core = core;
    }
    @Override
    public RemoteResponse handleWebRequest(RemoteRequest request) throws RemoteException
    {
        // Create response wrapper
        RemoteResponse response = new RemoteResponse();
        // Pass request to web-manager
        core.getWebManager().handleWebRequest(request, response);
        // Return wrapper...
        return response;
    }
    @Override
    public void pluginUnload(UUID plugin) throws RemoteException
    {
        if(plugin == null)
            return;
        Plugin p = core.getPlugins().getPlugin(plugin);
        if(p != null)
            core.getPlugins().unload(p);
    }
    @Override
    public boolean invokeGlobalHook(String event, Object[] data) throws RemoteException
    {
        return core.getPlugins().globalHookInvoke(event, data);
    }
    @Override
    public void invokeGlobalHookAll(String event, Object[] data) throws RemoteException
    {
        core.getPlugins().globalHookInvokeAll(event, data);
    }
    @Override
    public void restart() throws RemoteException
    {
        core.stop(NodeCore.StopType.Normal);
        core.start();
    }
    @Override
    public void shutdown() throws RemoteException
    {
        core.stop(NodeCore.StopType.Shutdown);
    }
}
