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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import static org.junit.Assert.*;
import org.junit.Test;
import pals.TestWithCore;

/**
 * Tests {@link PalsProcess}.
 * 
 * @version 1.0
 */
public class PalsProcessTest extends TestWithCore
{
    /**
     * Tests creating and executing the "java" process, with the working
     * directory set to the base-path of the test.
     * @throws IOException Thrown if the base-path cannot be determined.
     * 
     * @since 1.0
     */
    @Test
    public void testCreateRun() throws IOException
    {
        PalsProcess p = PalsProcess.create(core, new File("").getCanonicalPath(), "java", new String[0]);
        assertNotNull(p);
        
        assertNotNull(p.getProcessBuilder());
        assertNull(p.getProcess());
        
        assertTrue(p.start());
        assertFalse(p.hasExited());
        assertNotNull(p.getProcess());
        
        try
        {
            Thread.sleep(1000);
        }
        catch(InterruptedException ex)
        {
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getProcess().getInputStream()));
        String l;
        while((l = br.readLine()) != null)
            System.out.println("DIAG : " + l);
            
        assertTrue(p.hasExited());
    }
}
