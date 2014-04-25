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
import pals.TestWithCore;
import pals.base.auth.User;
import pals.base.database.Connector;

/**
 * Tests {@link WebRequestData].
 * 
 * @version 1.0
 */
public class WebRequestDataTest extends TestWithCore
{
    /**
     * Tests appending and template data.
     * 
     * @since 1.0
     */
    @Test
    public void testAppendTemplateData()
    {
        Connector conn = core.createConnector();
        RemoteRequest req = new RemoteRequest("sessid", "relurl", "localhost");
        RemoteResponse resp = new RemoteResponse();
        
        WebRequestData data = WebRequestData.create(core, conn, req, resp);
        
        data.setTemplateData("pals_header", null);
        assertNull(data.getTemplateData("pals_header"));
        data.appendHeader("test");
        assertNotNull(data.getTemplateData("pals_header"));
        
        data.setTemplateData("pals_header", null);
        assertNull(data.getTemplateData("pals_header"));
        data.appendHeaderCSS("test url");
        assertNotNull(data.getTemplateData("pals_header"));
        
        data.setTemplateData("pals_header", null);
        assertNull(data.getTemplateData("pals_header"));
        data.appendHeaderJS("test");
        assertNotNull(data.getTemplateData("pals_header"));
        
        conn.disconnect();
    }
    /**
     * Tests mutators and accessors.
     * 
     * @since 1.0
     */
    @Test
    public void testMutatorsAccessors()
    {
        Connector conn = core.createConnector();
        RemoteRequest req = new RemoteRequest("sessid", "relurl", "localhost");
        RemoteResponse resp = new RemoteResponse();
        
        WebRequestData data = WebRequestData.create(core, conn, req, resp);
        
        assertEquals(core, data.getCore());
        assertEquals(conn, data.getConnector());
        assertEquals(req, data.getRequestData());
        assertEquals(resp, data.getResponseData());
        assertNotNull(data.getTemplateMap());
        assertNotNull(data.getSession());
        
        User usr = new User();
        data.setUser(usr);
        assertEquals(usr, data.getUser());
        
        conn.disconnect();
    }
}
