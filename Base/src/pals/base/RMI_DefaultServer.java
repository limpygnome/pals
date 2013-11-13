package pals.base;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 *
 */
public class RMI_DefaultServer extends UnicastRemoteObject  implements RMI_Interface
{
    public RMI_DefaultServer() throws RemoteException
    {
        super();
    }

    @Override
    public int test(int a, int b)
    {
        return a+b;
    }
}
