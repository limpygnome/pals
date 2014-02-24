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
package pals.base;

import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
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
    // Methods - Constructors **************************************************
    public RMI(Connector conn, int port, RMI_Interface serverInstance)
    {
        this.port = port;
        this.registry = null;
        this.serverInstance = serverInstance;
        this.hosts = new HashMap<>();
        hostsUpdate(conn);
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
        Registry r;
        RMI_Interface ri;
        for(RMI_Host h : hosts.values())
        {
            try
            {
                r = LocateRegistry.getRegistry(h.getHost(), h.getPort());
                ri = (RMI_Interface)r.lookup(RMI_Interface.class.getName());
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
        Registry r;
        RMI_Interface ri;
        for(RMI_Host h : hosts.values())
        {
            try
            {
                r = LocateRegistry.getRegistry(h.getHost(), h.getPort());
                ri = (RMI_Interface)r.lookup(RMI_Interface.class.getName());
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
    // Methods *****************************************************************
    /**
     * Starts the RMI service.
     * @return True if successful, false if failed.
     */
    public boolean start()
    {
        if(registry != null)
            return false;
        try
        {
            registry = LocateRegistry.createRegistry(port);
            registry.bind(RMI_Interface.class.getName(), serverInstance);
            return true;
        }
        catch(RemoteException | AlreadyBoundException ex)
        {
            if(registry != null)
                disposeRegistry();
            return false;
        }
    }
    /**
     * Stops and disposes the RMI service.
     */
    public void stop()
    {
        if(registry == null)
            return;
        disposeRegistry();
    }
    private void disposeRegistry()
    {
        try
        {
            UnicastRemoteObject.unexportObject(registry, true);
        }
        catch(NoSuchObjectException ex) {}
        registry = null;
    }
}
