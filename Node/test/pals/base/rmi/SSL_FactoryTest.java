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
package pals.base.rmi;

import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;
import pals.TestWithCore;

/**
 * Tests {@link SSL_FactoryTest}. Note: core is requied for test, which
 * allows testing of the client socket, which connects on creation.
 * 
 * @version 1.0
 */
public class SSL_FactoryTest extends TestWithCore
{
    /**
     * Tests constructing a new factory.
     * 
     * @since 1.0
     */
    @Test
    public void testConstruct()
    {
        SSL_Factory sf = SSL_Factory.createFactory("pals.jks", "password");
        assertNotNull(sf);
    }
    /**
     * Tests creating client and server sockets.
     * 
     * @since 1.0
     */
    @Test
    public void tstCreateSockets()
    {
        SSL_Factory sf = SSL_Factory.createFactory("pals.jks", "password");
        assertNotNull(sf);
        
        try
        {
            assertNotNull(sf.createServerSocket(9000));
        }
        catch(IOException ex)
        {
            fail("Failed to setup SSL server socket.");
        }
        
        try
        {
            assertNotNull(sf.createSocket("localhost", 1099));
        }
        catch(IOException ex)
        {
            fail("Failed to setup SSL client socket.");
        }
    }
}
