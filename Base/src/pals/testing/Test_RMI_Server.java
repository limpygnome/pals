package pals.testing;

import java.rmi.RemoteException;
import pals.base.RMI;
import pals.base.RMI_DefaultServer;

public class Test_RMI_Server
{
    public static void main(String[] args) throws InterruptedException, RemoteException
    {
        RMI rmi = new RMI(1099, new RMI_DefaultServer());
        // Start the server
        if(!rmi.start())
        {
            System.err.println("Failed to start RMI registry/server...");
            return;
        }
        // Loop forever
        for(;;)
            Thread.sleep(999999);
    }
}
