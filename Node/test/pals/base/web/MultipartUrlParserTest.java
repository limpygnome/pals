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
package pals.base.web;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Tests {@link MultipartUrlParser}.
 * 
 * @version 1.0
 */
public class MultipartUrlParserTest
{
    /**
     * Performs all testing for class.
     * 
     * @since 1.0
     */
    @Test
    public void testAll()
    {
        MultipartUrlParser mup;
        
        mup = new MultipartUrlParser("a/b/c/d");
        assertEquals("a", mup.getPart(0));
        assertEquals("b", mup.getPart(1));
        assertEquals("c", mup.getPart(2));
        assertEquals("d", mup.getPart(3));
        
        mup = new MultipartUrlParser("/a/b/c/d/");
        assertEquals("a", mup.getPart(0));
        assertEquals("b", mup.getPart(1));
        assertEquals("c", mup.getPart(2));
        assertEquals("d", mup.getPart(3));
        
        mup = new MultipartUrlParser("a/b/2/3");
        assertEquals(-1, mup.parseInt(0,-1));
        assertEquals(-1, mup.parseInt(1,-1));
        assertEquals(2, mup.parseInt(2,-1));
        assertEquals(3, mup.parseInt(3,-1));
    }
}
