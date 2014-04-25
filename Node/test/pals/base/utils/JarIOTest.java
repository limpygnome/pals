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

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Tests {@link JarIO}.
 * 
 * @version 1.0
 */
public class JarIOTest
{
    /**
     * Tests opening a Java archive.
     * 
     * @throws JarIOException Thrown if an issue occurs with the jar.
     * @since 1.0
     */
    @Test
    public void testOpenAccessors() throws JarIOException
    {
        JarIO jio = JarIO.open("dist/PALS__Node.jar", null);
        assertNotNull(jio);
        
        assertNotNull(jio.getPath());
        assertNotNull(jio.getRawLoader());
    }
    /**
     * Tests fetching a class based on its full class-name.
     * 
     * @throws JarIOException Thrown if an issue occurs with the jar.
     * @since 1.0
     */
    @Test
    public void testFetchClass() throws JarIOException
    {
        JarIO jio = JarIO.open("dist/PALS__Node.jar", null);
        assertNotNull(jio);
        
        Class c = jio.fetchClassType(this.getClass().getName());
        assertNotNull(c);
        assertEquals(this.getClass(), c);
        
        try
        {
            c = jio.fetchClassType("dsofkodsfkdsifjkdfjdsfdsfijifgmnf");
            fail("No exception thrown for invalid class.");
        }
        catch(JarIOException ex)
        {
            assertTrue(true);
        }
    }
    /**
     * Tests fetching files.
     * 
     * @throws JarIOException Thrown if an issue occurs with the jar.
     * @since 1.0
     */
    @Test
    public void testFetchFiles() throws JarIOException
    {
        JarIO jio = JarIO.open("dist/PALS__Node.jar", null);
        assertNotNull(jio);
        
        String[] files = jio.getFiles(null, null, true, true);
        assertNotNull(files);
        assertTrue(files.length>0);
    }
}
