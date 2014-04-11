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

import java.util.HashMap;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests {@link TemplateManager}.
 * 
 * @version 1.0
 */
public class TemplateManagerTest
{
    private static NodeCore core;
    
    /**
     * Sets up and starts an instance of a core.
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
     * Disposes core.
     * 
     * @since 1.0
     */
    @AfterClass
    public static void dispose()
    {
        core.stop();
        assertTrue(core.getState() == NodeCore.State.Stopped);
    }
    
    @Test
    public void testReload()
    {
        TemplateManager tm = core.getTemplates();
        
        int total = tm.size();
        assertTrue(tm.reload());
        assertEquals(total, tm.size());
        
        tm.clear();
        assertTrue(tm.reload());
        assertEquals(total, tm.size());
    }
    
    @Test
    public void testLoading()
    {
        // A lot of the methods are already used by plugins, which throw
        // errors and stop the core from loading, which would be caught
        // in the setup method. Testing this by its self is overly
        // complicated and requires physical files. Therefore, the better
        // option is to not test this area.
    }
    /**
     * Tests registering, unregistering and rendering templates.
     * 
     * @since 1.0
     */
    @Test
    public void testRegisteringRendering()
    {
        Plugin p = new PluginTest.TestPlugin(core, UUID.generateVersion4(), null, null, null, null);
        TemplateManager tm = core.getTemplates();
        final String TEMPLATE_PATH = "unit-testing/test";
        final String TEMPLATE_DATA = "hello world.";
        
        tm.registerTemplate(p, TEMPLATE_PATH, TEMPLATE_DATA);
        assertTrue(tm.containsTemplate(TEMPLATE_PATH));
        
        HashMap<String,Object> hm = new HashMap<>();
        String result = tm.render(null, hm, TEMPLATE_PATH);
        assertEquals(TEMPLATE_DATA, result);
        
        tm.remove(p);
        assertFalse(tm.containsTemplate(TEMPLATE_PATH));
        
        tm.registerTemplate(p, TEMPLATE_PATH, TEMPLATE_DATA);
        assertTrue(tm.containsTemplate(TEMPLATE_PATH));
        
        tm.remove(TEMPLATE_PATH);
        assertFalse(tm.containsTemplate(TEMPLATE_PATH));
    }
    /**
     * Tests clearing and reloading templates.
     * 
     * @since 1.0
     */
    @Test
    public void testClear()
    {
        TemplateManager tm = core.getTemplates();
        
        tm.clear();
        assertTrue(tm.reload());
    }
}
