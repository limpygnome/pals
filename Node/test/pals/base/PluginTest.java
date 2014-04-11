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

import org.junit.Test;
import pals.base.database.Connector;
import pals.base.utils.JarIO;
import static org.junit.Assert.*;
import pals.base.web.WebRequestData;

/**
 * Tests {@link Plugin}.
 * 
 * @version 1.0
 */
public class PluginTest
{
    /**
     * A test plugin.
     * 
     * @version 1.0
     */
    public static class TestPlugin extends Plugin
    {
        public TestPlugin(NodeCore core, UUID uuid, JarIO jario, Version version, Settings settings, String jarPath)
        {
            super(core, uuid, jario, version, settings, jarPath);
        }
        @Override
        public boolean eventHandler_pluginInstall(NodeCore core, Connector conn)
        {
            return true;
        }
        @Override
        public boolean eventHandler_pluginUninstall(NodeCore core, Connector conn)
        {
            return true;
        }
        @Override
        public boolean eventHandler_pluginLoad(NodeCore core)
        {
            return true;
        }

        @Override
        public boolean eventHandler_webRequest(WebRequestData data)
        {
            data.getResponseData().setBuffer("hello world");
            return true;
        }    
    }
    /**
     * Tests the accessors of the plugin.
     * 
     * @since 1.0
     */
    @Test
    public void testPluginAccessors()
    {
        // Create test settings
        NodeCore c = NodeCore.getInstance();
        UUID uuid = UUID.generateVersion4();
        JarIO jio = null;
        Version v = new Version(1, 5, 8);
        Settings s = new Settings(true);
        String jarLocation = "test";
        
        // Create new instance of test
        TestPlugin tp = new TestPlugin(c, uuid, jio, v, s, jarLocation);
        
        // Check references
        assertEquals(uuid, tp.getUUID());
        assertEquals(jio, tp.getJarIO());
        assertEquals(v, tp.getVersion());
        assertEquals(s, tp.getSettings());
        assertEquals(jarLocation, tp.getJarLocation());
        assertEquals(tp.getCore(), c);
    }
}
