package pals.base;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
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
