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
        
        // Reset core for other tests
        c.resetShutdown();
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
        
        assertTrue(c.getState() != NodeCore.State.Started && c.getState() != NodeCore.State.Starting);
        assertNull(c.createConnector());
        
        c.start();
        assertEquals(NodeCore.State.Started, c.getState());
        
        assertNotNull(c.createConnector());
        
        c.stop();
        assertEquals(NodeCore.State.Stopped, c.getState());
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
        c.setPathPlugins("../Plugins");
        
        assertNull(c.getPlugins());
        assertNull(c.getTemplates());
        assertNull(c.getWebManager());
        assertNull(c.getLogging());
        assertNull(c.getSettings());
        assertNull(c.getRNG());
        assertNull(c.getRMI());
        assertNull(c.getNodeUUID());
        
        c.start();
        assertEquals(NodeCore.State.Started, c.getState());
        
        assertNotNull(c.getPlugins());
        assertNotNull(c.getTemplates());
        assertNotNull(c.getWebManager());
        assertNotNull(c.getLogging());
        assertNotNull(c.getSettings());
        assertNotNull(c.getRNG());
        assertNotNull(c.getRMI());
        assertNotNull(c.getNodeUUID());
        
        c.stop();
        assertEquals(NodeCore.State.Stopped, c.getState());
        
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
