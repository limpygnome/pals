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
    Authors:    Marcus Craske           <limpygnome@gmail.com>
    ----------------------------------------------------------------------------
*/
package pals.base.rmi;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import static org.junit.Assert.*;
import org.junit.Test;
import pals.TestWithCore;
import pals.base.database.Connector;

/**
 * Tests {@link RMI} and {@link RMI_DefaultServer}.
 * 
 * Some areas cannot be tested, such as the global events mechanism,
 * because this is too complex for unit tests, at least within the given
 * time constraints.
 * 
 * @version 1.0
 */
public class RMI_RMIDefaultServer_Test extends TestWithCore
{
    /**
     * Tests updating the internal cache of nodes.
     * 
     * @since 1.0
     */
    @Test
    public void testHostsUpdate()
    {
        RMI r = core.getRMI();
        Connector conn = core.createConnector();
        
        // Make call
        r.hostsUpdate(conn);
        // Check the array is valid
        // -- We cannot check the actual array bcause the testing could occur on a cluster
        RMI_Host[] arr = r.getNodes();
        assertNotNull(arr);
        assertTrue(arr.length>0);
        
        conn.disconnect();
    }
    /**
     * Tests fetching connections.
     * 
     * @since 1.0
     */
    @Test
    public void testFetchConnection()
    {
        // Create server
        Registry r = core.getRMI().fetchRMIServerConnection(9923);
        assertNotNull(r);
        
        // Create client to connect to server
        try
        {
            RMI_Interface ri = core.getRMI().fetchRMIConnection("localhost", 9923);
            assertTrue(true); // For logging purposes
        }
        catch(NotBoundException | RemoteException ex)
        {
            fail(ex.getMessage());
        }
        
        // Dispose server
        RMI.disposeRegistry(r);
        assertTrue(true); // For logging purposes
    }
    /**
     * Tests stopping and starting RMI. May be required by plugins.
     * 
     * @since 1.0
     */
    @Test
    public void testStopStart()
    {
        RMI r = core.getRMI();
        
        r.stop();
        assertTrue(true);
        
        assertTrue(r.start());
    }
    /**
     * Tests restarting and shutting down a node using RMI.
     * 
     * @since 1.0
     */
    public void testNodeRestartShutdown()
    {
        // Restart node
        try
        {
            RMI_Interface ri = core.getRMI().fetchRMIConnection("localhost", 1099);
            ri.restart();
            assertTrue(true);
        }
        catch(NotBoundException | RemoteException ex)
        {
            fail(ex.getMessage());
        }
        // Shutdown node
        try
        {
            RMI_Interface ri = core.getRMI().fetchRMIConnection("localhost", 1099);
            ri.shutdown();
            assertTrue(true);
        }
        catch(NotBoundException | RemoteException ex)
        {
            fail(ex.getMessage());
        }
        // Restart manually
        core.resetShutdown();
        assertTrue(core.start());
    }
}
