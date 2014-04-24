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
package pals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;

/**
 * Extensions for unit testing.
 * 
 * @version 1.0
 */
public class PalsUnitExtensions
{
    /**
     * An extension for JUnit for testing if two arrays are equal, but with
     * the same elements - allows a different order of elements.
     * 
     * @param <T> Type of arrays.
     * @param expected Expected array.
     * @param actual Actual array.
     * @since 1.0
     */
    public static <T extends Comparable> void assertArrayElementsEqual(T[] expected, T[] actual)
    {
        if(expected == null && actual == null)
            return; // The same...
        else if((expected == null || actual == null) || expected.length != actual.length)
            // Impossible to be the same
            fail("Expected '"+safeArrayPrint(expected)+"', actually '"+actual+"'.");
        else
        {
            // Convert to lists and sort
            List<T> arrE = Arrays.asList(expected),
                    arrA = Arrays.asList(actual);
            
            Collections.sort(arrE);
            Collections.sort(arrA);
            
            for(int i = 0; i < arrE.size(); i++)
            {
                if(!arrE.get(i).equals(arrA.get(i)))
                    fail("Expected '"+safeArrayPrint(expected)+"', actually '"+actual+"'.");
            }
            // For logging purposes, this test has been successful...
            assertTrue(true);
        }
    }
    private static <T> String safeArrayPrint(T[] v)
    {
        if(v == null)
            return "null";
        StringBuilder sb = new StringBuilder();
        for(T vv : v)
            sb.append(vv).append(",");
        if(sb.length() > 0)
            sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }
}
