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

import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import pals.base.NodeCore;
import pals.base.UUID;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.database.Result;

/**
 * Used for RMI (remote method invocation) communication; allows methods to be
 * invoked over the network. Useful for inter-communication between different
 * Java Virtual Machines.
 */
public class RMI
{
    // Fields ******************************************************************
    private int                     port;
    private Registry                registry;
    private final RMI_Interface     serverInstance;
    private HashMap<UUID,RMI_Host>  hosts;
    private SSL_Factory             sockFactory;
    // Methods - Constructors **************************************************
    public RMI(NodeCore core, Connector conn, int port, RMI_Interface serverInstance)
    {
        this.port = port;
        this.registry = null;
        this.serverInstance = serverInstance;
        this.hosts = new HashMap<>();
        hostsUpdate(conn);
        // Setup custom SSL factory if keystore settings are defined
        String  keystorePath = core.getSettings().getStr("rmi/keystore/path"),
                keystorePassword = core.getSettings().getStr("rmi/keystore/password");
        if(keystorePath != null && keystorePassword != null)
            sockFactory = new SSL_Factory(keystorePath, keystorePassword);
        else
            sockFactory = null;
        
    }
    // Methods - Hosts *********************************************************
    /**
     * Updates the local cache of RMI-enabled nodes.
     * 
     * @param conn Database connector.
     */
    public synchronized void hostsUpdate(Connector conn)
    {
        try
        {
            // Clear old hosts
            hosts.clear();
            // Fetch hosts
            Result res = conn.read("SELECT * FROM pals_nodes WHERE rmi_ip IS NOT NULL AND rmi_port IS NOT NULL;");
            // Add to local cache
            UUID uuid;
            RMI_Host h;
            while(res.next())
            {
                uuid = UUID.parse((byte[])res.get("uuid_node"));
                h = new RMI_Host(uuid, (String)res.get("rmi_ip"), (int)res.get("rmi_port"));
                hosts.put(uuid, h);
            }
        }
        catch(DatabaseException ex)
        {
        }
    }
    /**
     * Invokes a global event on all nodes.
     * 
     * @param event The name of the global event.
     * @param data The data to be passed to plugins; must be serializable.
     * @return Indicates if a plugin on a host handled the event.
     */
    public synchronized boolean nodesGlobalEvent(String event, Object[] data)
    {        
        RMI_Interface ri;
        for(RMI_Host h : hosts.values())
        {
            try
            {
                ri = fetchRMIConnection(h.getHost(), h.getPort());
                return ri.invokeGlobalHook(event, data);
            }
            catch(NotBoundException | RemoteException ex)
            {
            }
        }
        return false;
    }
    /**
     * Invokes a global event on all nodes.
     * 
     * @param event The name of the global event.
     * @param data The data to be passed to plugins; must be serializable.
     */
    public synchronized void nodesGlobalEventAll(String event, Object[] data)
    {
        RMI_Interface ri;
        for(RMI_Host h : hosts.values())
        {
            try
            {
                ri = fetchRMIConnection(h.getHost(), h.getPort());
                ri.invokeGlobalHookAll(event, data);
            }
            catch(NotBoundException | RemoteException ex)
            {
            }
        }
    }
    /**
     * @return All of the RMI-enabled nodes cached.
     */
    public synchronized RMI_Host[] getNodes()
    {
        return hosts.values().toArray(new RMI_Host[hosts.size()]);
    }
    // Methods - RMI Connections ***********************************************
    /**
     * Fetches an instance of an RMI_Interface for inter-node communication.
     * 
     * @param host The hostname/IP of the node.
     * @param port The port of the RMI registry of the node.
     * @return An instance of an interface to communicate with a remote node.
     * @throws NotBoundException Thrown if a remote interface is not compatible.
     * @throws RemoteException Thrown if a connection cannot be established
     * with the node.
     */
    public synchronized RMI_Interface fetchRMIConnection(String host, int port) throws NotBoundException, RemoteException
    {
        Registry r = sockFactory != null ? LocateRegistry.getRegistry(host, port, sockFactory) : LocateRegistry.getRegistry(host, port); //new SslRMIClientSocketFactory());
        return (RMI_Interface)r.lookup(RMI_Interface.class.getName());
    }
    /**
     * Creates an RMI registry / server connection.
     * 
     * @param port The port on which the registry will listen.
     * @return An instance of a registry or null.
     */
    public synchronized Registry fetchRMIServerConnection(int port)
    {
        try
        {
            Registry r = sockFactory != null ? LocateRegistry.createRegistry(port, sockFactory, sockFactory) : LocateRegistry.createRegistry(port);;
            r.bind(RMI_Interface.class.getName(), serverInstance);
            return r;
        }
        catch(RemoteException | AlreadyBoundException ex)
        {
            return null;
        }
    }
    // Methods *****************************************************************
    /**
     * Starts the RMI service.
     * @return True if successful, false if failed.
     */
    public boolean start()
    {
        if(registry != null)
            return false;
        if((registry = fetchRMIServerConnection(port)) == null)
            return false;
        return true;
    }
    /**
     * Stops and disposes the RMI service.
     */
    public void stop()
    {
        if(registry == null)
            return;
        disposeRegistry(registry);
        registry = null;
    }
    /**
     * Disposes a registry / RMI server connection.
     * 
     * @param r The registry to be disposed.
     */
    public void disposeRegistry(Registry r)
    {
        try
        {
            UnicastRemoteObject.unexportObject(r.lookup(RMI_Interface.class.getName()), true);
        }
        catch(RemoteException | NotBoundException ex)
        {
            ex.printStackTrace(System.err);
        }
    }
}
