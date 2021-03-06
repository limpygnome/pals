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
package pals.base.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import static org.junit.Assert.*;
import org.junit.Test;
import pals.TestWithCore;
import pals.base.PluginTest;
import pals.base.UUID;

/**
 * Tests {@link PluginObjectInputStreamTest}.
 * 
 * @version 1.0
 */
public class PluginObjectInputStreamTest extends TestWithCore
{
    /**
     * Tests resolving a class from a plugin.
     * 
     * @since 1.0
     */
    @Test
    public void testResolveClass()
    {
        // Add plugin to system
        PluginTest.TestPlugin tp = new PluginTest.TestPlugin(core, UUID.generateVersion4(), null, null, null, null);
        core.getPlugins().add(tp);
        
        // Serialize test object
        PluginTest.TestPlugin.TestClass tc = new PluginTest.TestPlugin.TestClass();
        byte[] data;
        try
        {
            data = Misc.bytesSerialize(tc);
            assertTrue(true);
        }
        catch(IOException ex)
        {
            fail("Failed to serialize data - " + ex.getMessage());
            return;
        }
        // Deserialize object
        try
        {
            PluginObjectInputStream pois = new PluginObjectInputStream(core, new ByteArrayInputStream(data));
            Object obj = pois.readObject();
            assertNotNull(obj);
            assertEquals(tc, obj);
        }
        catch(IOException | ClassNotFoundException ex)
        {
            fail(ex.getMessage());
        }
    }
}
