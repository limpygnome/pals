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

import java.io.UnsupportedEncodingException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import pals.base.web.RemoteRequest;
import pals.base.web.RemoteResponse;
import static org.junit.Assert.*;
import org.junit.Test;
import pals.TestWithCore;

/**
 * Tests the {@link WebManager}.
 * 
 * Note: this test is very fragile, since this is quite a complex component.
 * 
 * @version 1.0
 */
public class WebManagerTest extends TestWithCore
{
    /**
     * Tests registering/urnegistering and serving a URL.
     * 
     * @since 1.0
     */
    @Test
    public void testRegisterUnregisterRequest() throws IllegalArgumentException, UnsupportedEncodingException
    {
        Plugin p = new PluginTest.TestPlugin(core, UUID.generateVersion4(), null, null, null, null);
        WebManager wm = core.getWebManager();
        
        // Add plugin illegally into plugin manager
        core.getPlugins().add(p);
        
        // Register plugin
        assertTrue(wm.urlsRegister(p, new String[]{"/unit-testing-url"}));
        
        // Create request
        RemoteRequest req = new RemoteRequest(null, "/unit-testing-url", "127.0.0.1");
        RemoteResponse rep = new RemoteResponse();
        
        wm.handleWebRequest(req, rep);
        
        String data = new String(rep.getBuffer(), "UTF-8");
        assertEquals("hello world", data);
        
        // Unregister plugin
        wm.urlsUnregister(p);
    }
    /**
     * Tests reloading all the URLs.
     * 
     * @since 1.0
     */
    @Test
    public void testReload()
    {
        WebManager wm = core.getWebManager();
        
        int urls = wm.getUrlTree().getUrls().length;
        
        assertTrue(wm.reload());
        
        assertEquals(urls, wm.getUrlTree().getUrls().length);
    }
}
