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
package pals.web.security;

import static org.junit.Assert.*;
import org.junit.Test;
import pals.TestWithCore;
import pals.base.database.Connector;
import pals.base.web.RemoteRequest;
import pals.base.web.RemoteResponse;
import pals.base.web.WebRequestData;
import pals.base.web.security.CSRF;

/**
 * Tests {@link CSRF}.
 * 
 * @version 1.0
 */
public class CSRFTest extends TestWithCore
{
    @Test
    public void testCSRF()
    {
        Connector conn = core.createConnector();
        WebRequestData w = WebRequestData.create(core, conn, new RemoteRequest(null, "/", "127.0.0.1"), new RemoteResponse());
        
        // Ensure we are not safe
        assertFalse(CSRF.isSecure(w));
        
        // Set CSRF
        String token = CSRF.set(w);
        
        // Attempt to redeem
        assertTrue(CSRF.isSecure(w, token));
        
        // Grab a new token
        String token2 = CSRF.set(w);
        
        // Attempt with old token
        assertFalse(CSRF.isSecure(w, token));
        
        // Attempt with new
        assertTrue(CSRF.isSecure(w, token2));
        
        conn.disconnect();
    }
}
