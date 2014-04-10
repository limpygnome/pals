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

import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests {@link PluginManager}.
 * 
 * @version 1.0
 */
public class PluginManagerTest
{
    private static NodeCore core;
    
    /**
     * Setups a core for the tests.
     * 
     * @since 1.0
     */
    @BeforeClass
    public static void setup()
    {
        core = NodeCore.getInstance();
        core.setPathPlugins("../Plugins");
        core.start();
        assertTrue(core.getState() == NodeCore.State.Started);
    }
    /**
     * Disposes the core.
     * 
     * @since 1.0
     */
    @AfterClass
    public static void dispose()
    {
        core.stop(NodeCore.StopType.Shutdown);
        assertTrue(core.getState() == NodeCore.State.Shutdown);
        core = null;
    }
    
    /**
     * Tests the global hook mechanism.
     * 
     * @since 1.0
     */
    @Test
    public void testGlobalHooks()
    {
        final String HOOK_NAME = "dsdsjfidjfs-unit-testing";
        PluginManager pm = core.getPlugins();
        // Fetch the first available plugin
        Plugin p = pm.getPlugins()[0];
        // Register hook with obscure name
        assertTrue(pm.globalHookRegister(p, HOOK_NAME));
        // Fetch the plugins for the hook
        Plugin[] ps = pm.globalHookFetch(HOOK_NAME);
        assertEquals(1, ps.length);
        assertEquals(p, ps[0]);
        // Unregister hook
        pm.globalHookUnregister(p, HOOK_NAME);
        // Check it's no longer registered
        ps = pm.globalHookFetch(HOOK_NAME);
        assertEquals(0, ps.length);
    }
    /**
     * Tests the plugin loading/unloading mechanism.
     * 
     * @since 1.0
     */
    @Test
    public void testPluginLoading()
    {
        PluginManager pm = core.getPlugins();
        int plugins = pm.getPlugins().length;
        
        // Reload plugins
        assertTrue(pm.reload(core.createConnector()));
        // Check the same number of items exist
        assertEquals(plugins, pm.getPlugins().length);
        
        // We cannot test loading/unloading a plugin because it's too complicated
        // to be safely tested at this level.
    }
    /**
     * Tests the accessors.
     * 
     * @since 1.0
     */
    @Test
    public void testAccessors()
    {
        PluginManager pm = core.getPlugins();
        Plugin[] plugins = pm.getPlugins();
        
        assertTrue(plugins.length > 0);
        
        assertEquals(plugins[0], pm.getPlugin(plugins[0].getUUID()));
    }
}
