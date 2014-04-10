

package pals.base;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Tests {@link NodeCore}.
 * 
 * @version 1.0
 */
public class NodeCoreTest
{
    /**
     * Tests starting, stopping and the state of an instance.
     * 
     * @since 1.0
     */
    @Test
    public void testStartStopState()
    {
        NodeCore c = NodeCore.getInstance();
        c.setPathPlugins("../Plugins");
        c.start();
        assertTrue(c.getState() == NodeCore.State.Started);
        c.stop();
        assertTrue(c.getState() == NodeCore.State.Stopped);
        c.start();
        assertTrue(c.getState() == NodeCore.State.Started);
        c.stop(NodeCore.StopType.Shutdown);
        assertTrue(c.getState() == NodeCore.State.Shutdown);
    }
    /**
     * Tests creating a database connector.
     * 
     * @since 1.0
     */
    @Test
    public void testCreateConnector()
    {
        NodeCore c = NodeCore.getInstance();
        c.setPathPlugins("../Plugins");
        
        assertNull(c.createConnector());
        
        c.start();
        
        assertNotNull(c.createConnector());
        
        c.stop();
    }
    /**
     * Tests the facade accessors in different states.
     * 
     * @since 1.0
     */
    @Test
    public void testFacadeAccessors()
    {
        NodeCore c = NodeCore.getInstance();
        
        assertNull(c.getPlugins());
        assertNull(c.getTemplates());
        assertNull(c.getWebManager());
        assertNull(c.getLogging());
        assertNull(c.getSettings());
        assertNull(c.getRNG());
        assertNull(c.getRMI());
        assertNull(c.getNodeUUID());
        
        c.start();
        
        assertNotNull(c.getPlugins());
        assertNotNull(c.getTemplates());
        assertNotNull(c.getWebManager());
        assertNotNull(c.getLogging());
        assertNotNull(c.getSettings());
        assertNotNull(c.getRNG());
        assertNotNull(c.getRMI());
        assertNotNull(c.getNodeUUID());
        
        c.stop();
        
        assertNull(c.getPlugins());
        assertNull(c.getTemplates());
        assertNull(c.getWebManager());
        assertNull(c.getLogging());
        assertNull(c.getSettings());
        assertNull(c.getRNG());
        assertNull(c.getRMI());
        assertNull(c.getNodeUUID());
    }
}
