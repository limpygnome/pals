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

import java.io.IOException;
import java.util.Random;
import org.joda.time.DateTime;
import static org.junit.Assert.*;
import org.junit.Test;
import pals.PalsUnitExtensions;

/**
 * Tests {@link Misc}.
 * 
 * @since 1.0
 */
public class MiscTest
{
    /**
     * Tests any array methods.
     * 
     * @since 1.0
     */
    @Test
    public void testArrays()
    {
        assertArrayEquals(new String[]{"d"}, Misc.arrayStringUnique(new String[]{"d","d","d","d"}));
        
        assertArrayEquals(new String[]{"a","b"}, Misc.arrayStringNonEmpty(new String[]{"a","",null,"b"}));
        
        String[] arr = new String[]{"c","b","a"};
        Misc.arrayShuffle(new Random(System.currentTimeMillis()), arr);
        PalsUnitExtensions.assertArrayElementsEqual(new String[]{"a","b","c"}, arr);
        
        arr = new String[]{"b","a","c"};
        Misc.arraySwap(arr, 0, 1);
        assertArrayEquals(new String[]{"a","b","c"}, arr);
        
        assertTrue(Misc.arrayContains(new String[]{"a","b","c"}, "a"));
        assertFalse(Misc.arrayContains(new String[]{"a","b","c"}, "d"));
        
        assertArrayEquals(new String[]{"a","b","c","d"}, Misc.arrayMerge(String.class, new String[]{"a","b"}, new String[]{"c","d"}));
    }
    /**
     * Tests alpha numeric character validation methods.
     * 
     * @since 1.0
     */
    @Test
    public void testAlphaNumeric()
    {
        assertTrue(Misc.isAlphaNumeric("abcd1234"));
        assertFalse(Misc.isAlphaNumeric("afvd.ds"));
        
        assertTrue(Misc.isNumeric("4332432"));
        assertFalse(Misc.isNumeric("2343432O"));
    }
    /**
     * Tests {@link Misc#bytesSerialize(java.lang.Object)} and
     * {@link Misc#bytesDeserialize(byte[])}.
     * 
     * @since 1.0
     */
    @Test
    public void testSerialization()
    {
        Integer t = 2;
        byte[] data;
        // Serialize
        try
        {
            data = Misc.bytesSerialize(t);
            assertTrue(true);
        }
        catch(IOException ex)
        {
            fail(ex.getMessage());
            return;
        }
        // Deserialize
        try
        {
            Object o = Misc.bytesDeserialize(data);
            assertNotNull(o);
            assertTrue(o instanceof Integer);
            assertEquals(2, o);
        }
        catch(ClassNotFoundException | IOException ex)
        {
            fail(ex.getMessage());
        }
    }
    /**
     * Tests {@link Misc#humanDateTime(org.joda.time.DateTime)}.
     * 
     * @since 1.0
     */
    @Test
    public void testDateTime()
    {
        assertEquals("1 second ago", Misc.humanDateTime(DateTime.now().minusSeconds(1)));
        assertEquals("2 seconds ago", Misc.humanDateTime(DateTime.now().minusSeconds(2)));
        
        assertEquals("1 minute ago", Misc.humanDateTime(DateTime.now().minusMinutes(1)));
        assertEquals("2 minutes ago", Misc.humanDateTime(DateTime.now().minusMinutes(2)));
        
        assertEquals("1 hour ago", Misc.humanDateTime(DateTime.now().minusHours(1)));
        assertEquals("2 hours ago", Misc.humanDateTime(DateTime.now().minusHours(2)));
        
        assertEquals("1 month ago", Misc.humanDateTime(DateTime.now().minusMonths(1)));
        assertEquals("2 months ago", Misc.humanDateTime(DateTime.now().minusMonths(2)));
    }
    /**
     * Tests {@link Misc#countOccurrences(java.lang.String, char)}.
     * 
     * @since 1.0
     */
    @Test
    public void testCountOccurrences()
    {
        assertEquals(0, Misc.countOccurrences("abc", 'd'));
        assertEquals(1, Misc.countOccurrences("abc\nd", 'd'));
        assertEquals(3, Misc.countOccurrences("\nd\nabcdd", 'd'));
    }
    /**
     * Tests {@link Misc#parseInt(java.lang.String, int)}.
     * 
     * @since 1.0
     */
    @Test
    public void testParse()
    {
        assertEquals(1, Misc.parseInt("1", -1));
        assertEquals(-1, Misc.parseInt("1dsadsa", -1));
        assertEquals(-1, Misc.parseInt("one", -1));
        assertEquals(-1, Misc.parseInt("0F", -1));
        assertEquals(1343, Misc.parseInt("1343", -1));
    }
    /**
     * Tests {@link Misc#rngRange(java.util.Random, int, int)}.
     * 
     * @since 1.0
     */
    @Test
    public void testRngRange()
    {
        Random rng = new Random(System.currentTimeMillis());
        int t;
        
        t = Misc.rngRange(rng, 1, 5);
        assertTrue(t > 0);
        
        t = Misc.rngRange(rng, 0, 0);
        assertEquals(0, t);
        
        t = Misc.rngRange(rng, -1, -5);
        assertTrue(t < 0);
        
        t = Misc.rngRange(rng, -1, 5);
        for(int i = 0; i < 255; i++)
            assertTrue(t >= -1 && t <= 5);
        
        
        t = Misc.rngRange(rng, 1, -5);
        for(int i = 0; i < 255; i++)
            assertTrue(t >= -5 && t <= 1);
        
        t = Misc.rngRange(rng, 0, -5);
        for(int i = 0; i < 255; i++)
            assertTrue(t >= -5 && t <= 0);
    }
}
