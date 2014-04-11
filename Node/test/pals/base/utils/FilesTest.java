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

import java.io.File;
import java.io.IOException;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Tests {@link Files}.
 * 
 * @version 1.0
 */
public class FilesTest
{
    /**
     * Tests copying files.
     * 
     * @since 1.0
     */
    @Test
    public void testFileCopyReadWrite() throws IOException
    {
        // Create test file
        Files.fileWrite("unit_test_fcpy", "test data", true);
        
        // Copy
        Files.fileCopy("unit_test_fcpy", "unit_test_fcpy2", true);
        
        // Read contents and check it's the same
        assertEquals("test data", Files.fileRead("unit_test_fcpy2"));
        
        // Check both files exist
        File    f1 = new File("unit_test_fcpy"),
                f2 = new File("unit_test_fcpy2");
        assertTrue(f1.exists());
        assertTrue(f2.exists());
        
        // Delete
        assertTrue(f1.delete());
        assertTrue(f2.delete());
    }
}
