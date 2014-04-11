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
import pals.base.UUID;

/**
 * Tests {@link UUID}.
 * 
 * @version 1.0
 */
public class UUIDTest
{
    /**
     * Tests parsing UUIDs using strings. This also tests isValid method.
     * 
     * @since 1.0
     */
    @Test
    public void testParseString()
    {
        UUID t;
        
        t = UUID.parse("fd25b851-9e2b-4503-b570-0508c8b82d38");
        assertNotNull(t);
        
        t = UUID.parse("fdsdjsfdsfds");
        assertNull(t);
        
        t = UUID.parse("8214602b-f0a8-4ff2-81be-f03233167782");
        assertNotNull(t);
        
        t = UUID.parse("8214602b-f0a8-4ff2-81be-f0323316778z"); // Ends with Z - hex only!
        assertNull(t);
        
        t = UUID.parse("5918E5F0-C1A7-11E3-8A33-0800200C9A66");
        assertNotNull(t);
        
        t = UUID.parse("5918E5F0-C1A7-11E3-8A33-0800200C9A6Z"); // Capital Z
        assertNull(t);
    }
    /**
     * Tests parsing using bytes.
     * 
     * @since 1.0
     */
    @Test
    public void testParseBytes()
    {
        final String STR_UUID = "799412b1-0841-4598-827f-69eb60d97fc9";
        UUID t = UUID.parse(STR_UUID);
        byte[] data = t.getBytes();
        
        assertEquals(16, data.length);
        
        t = UUID.parse(data);
        assertNotNull(t);
        assertEquals(STR_UUID.toUpperCase(), t.getHexHyphens());
    }
    
    /**
     * Tests generating a random UUID.
     * 
     * @since 1.0
     */
    @Test
    public void testGenerateVersion4()
    {
        UUID uuid = UUID.generateVersion4();
        assertNotNull(uuid);
        assertEquals(16, uuid.getBytes().length);
        assertEquals(32, uuid.getHex().length());
    }
    /**
     * Tests the accessors.
     * 
     * @since 1.0
     */
    @Test
    public void testAccessors()
    {
        final String UUID_STR = "522EEB80-C1A8-11E3-8A33-0800200C9A66";
        final String UUID_STR_NH = "522EEB80C1A811E38A330800200C9A66";
        
        UUID uuid = UUID.parse(UUID_STR);
        assertNotNull(uuid);
        assertEquals(uuid.getHexHyphens(), UUID_STR);
        assertEquals(uuid.getHex(), UUID_STR_NH);
    }
    /**
     * Tests the equality override.
     * 
     * @since 1.0
     */
    @Test
    public void testEquals()
    {
        final String UUID_STR = "A2F6DA00-C1A8-11E3-8A33-0800200C9A66";
        UUID a = UUID.parse(UUID_STR);
        UUID b = UUID.parse(UUID_STR);
        
        assertTrue(a.equals(b));
        assertEquals(a, b);
        
        assertFalse(a.equals(UUID.generateVersion4()));
        assertNotEquals(a, UUID.generateVersion4());
        
        assertFalse(b.equals(UUID.generateVersion4()));
        assertNotEquals(b, UUID.generateVersion4());
    }
}
