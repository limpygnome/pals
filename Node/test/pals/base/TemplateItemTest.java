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
 * Tests {@link TemplateItem}.
 * 
 * @version 1.0
 */
public class TemplateItemTest
{
    /**
     * Tests the constructor and accessors.
     * 
     * @since 1.0
     */
    @Test
    public void testConstructor()
    {
        UUID uuid = UUID.generateVersion4();
        String path = "test/path";
        String data = "test template data";
        
        TemplateItem ti = new TemplateItem(uuid, path, data);
        
        assertEquals(uuid, ti.getPluginUUID());
        assertEquals(path, ti.getPath());
        assertEquals(data, ti.getData());
        
        assertTrue(ti.getLastModified() > System.currentTimeMillis()-200 && ti.getLastModified() <= System.currentTimeMillis());
    }
}
