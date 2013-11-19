package pals.base;

import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

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
    // Methods - Constructors **************************************************
    public RMI(int port, RMI_Interface serverInstance)
    {
        this.port = port;
        this.registry = null;
        this.serverInstance = serverInstance;
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
