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
 * Tests {@link UrlTreeTest}.
 * 
 * @version 1.0
 */
public class UrlTreeTest
{
    /**
     * Tests adding URLs.
     * 
     * @since 1.0
     */
    @Test
    public void testAddGetReset()
    {
        Plugin p = new PluginTest.TestPlugin(null, UUID.generateVersion4(), null, null, null, null);
        UrlTree ut = new UrlTree();
        
        // Check the tree can report it's empty
        assertEquals(0, ut.getUrls().length);
        
        // Test a single node
        assertEquals(UrlTree.RegisterStatus.Success, ut.add(p, "test/path"));
        assertEquals(1, ut.getUrls().length);
        assertEquals("/test/path", ut.getUrls()[0]);
       
        
        assertEquals(UrlTree.RegisterStatus.Failed_AlreadyExists, ut.add(p, "test/path"));
        
        // Test multiple nodes on a node
        assertEquals(UrlTree.RegisterStatus.Success, ut.add(p, "test/a"));
        assertEquals(UrlTree.RegisterStatus.Success, ut.add(p, "test/b"));
        assertEquals(UrlTree.RegisterStatus.Success, ut.add(p, "test/c"));
        assertEquals(4, ut.getUrls().length);
        
        // Test malformed node
        assertEquals(UrlTree.RegisterStatus.Failed_Malformed, ut.add(p, "             "));
        
        // Test different dirs
        assertEquals(UrlTree.RegisterStatus.Success, ut.add(p, "a"));
        assertEquals(UrlTree.RegisterStatus.Success, ut.add(p, "b"));
        assertEquals(UrlTree.RegisterStatus.Success, ut.add(p, "b/a"));
        
        // Test resetting
        ut.reset();
        assertEquals(0, ut.getUrls().length);
        
        assertEquals(UrlTree.RegisterStatus.Success, ut.add(p, "test/path"));
        assertEquals(1, ut.getUrls().length);
    }
    /**
     * Tests removing URLs.
     * 
     * @since 1.0
     */
    @Test
    public void testRemove()
    {
        Plugin p = new PluginTest.TestPlugin(null, UUID.generateVersion4(), null, null, null, null);
        UrlTree ut = new UrlTree();
        
        assertEquals(0, ut.getUrls().length);
        
        assertEquals(UrlTree.RegisterStatus.Success, ut.add(p, "test/path"));
        assertEquals(1, ut.getUrls().length);
        
        ut.remove(p);
        assertEquals(0, ut.getUrls().length);
    }
    /**
     * Tests fetching UUIDs.
     * 
     * @since 1.0
     */
    @Test
    public void testGetUUIDs()
    {
        Plugin p = new PluginTest.TestPlugin(null, UUID.generateVersion4(), null, null, null, null);
        UrlTree ut = new UrlTree();
        
        assertEquals(UrlTree.RegisterStatus.Success, ut.add(p, "hello/world"));
        
        assertArrayEquals(new UUID[]{p.getUUID()}, ut.getUUIDs("hello/world"));
        
        assertArrayEquals(new UUID[0], ut.getUUIDs("test/dsadas"));
        
        ut.reset();
        assertArrayEquals(new UUID[0], ut.getUUIDs("hello/world"));
    }
}
