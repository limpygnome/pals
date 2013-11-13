
package pals.base;

import java.rmi.*;

/**
 * The interface used for RMI communication.
 */
public interface RMI_Interface extends Remote
{
    public int test(int a, int b) throws RemoteException;
}
