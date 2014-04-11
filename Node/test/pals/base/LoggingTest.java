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

import java.io.File;
import java.io.IOException;
import org.junit.AfterClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import pals.TestWithCore;

/**
 * Tests {@link Logging}.
 * 
 * @version 1.0
 */
public class LoggingTest extends TestWithCore
{
    /**
     * Checks instances of logging can be created and disposed, as well as the
     * path working.
     * 
     * @since 1.0
     */
    @Test
    public void testCreateInstance()
    {
        Logging l = Logging.createInstance(core, "unit-test-"+Math.random()*10000, true, null);
        assertNotNull(l);
        assertNotNull(l.getPath());
        assertTrue(new File(l.getPath()).exists());
        l.dispose();
    }
    /**
     * Checks stack-trace logging can be enabled/disabled.
     * 
     * @since 1.0
     */
    @Test
    public void testStackTraces()
    {
        Logging l = Logging.createInstance(core, "unit-test-"+Math.random()*10000, true, null);
        assertTrue(l.isLoggingStackTraces());
        l.setStackTraces(false);
        assertFalse(l.isLoggingStackTraces());
        l.dispose();
    }
    /**
     * Tests {@link Logging#logEx(java.lang.String, java.lang.Throwable, pals.base.Logging.EntryType)}
     * works.
     * 
     * @since 1.0
     */
    @Test
    public void testLogEx3()
    {
        Logging l = Logging.createInstance(core, "unit-test-"+Math.random()*10000, true, null);
        assertTrue(
                l.logEx("test", new IOException("unit test"), Logging.EntryType.Info)
        );
        l.dispose();
    }
    /**
     * Tests {@link  Logging#logEx(java.lang.String, java.lang.String, java.lang.Throwable, pals.base.Logging.EntryType)}
     * works, as well as the entry-type filters.
     * 
     * @since 1.0
     */
    @Test
    public void testLogEx4()
    {
        Logging l = Logging.createInstance(core, "unit-test-"+Math.random()*10000, true, null);
        assertTrue(
                l.logEx("test", "test message", new IOException("unit test"), Logging.EntryType.Info)
        );
        assertTrue(
                l.logEx("test", "test message", new IOException("unit test"), Logging.EntryType.Warning)
        );
        assertTrue(
                l.logEx("test", "test message", new IOException("unit test"), Logging.EntryType.Error)
        );
        l.dispose();
        
        l = Logging.createInstance(core, "unit-test-"+Math.random()*10000, true, Logging.EntryType.getSet("Info,Warning,Error"));
        assertTrue(
                l.logEx("test", "test message", new IOException("unit test"), Logging.EntryType.Info)
        );
        assertTrue(
                l.logEx("test", "test message", new IOException("unit test"), Logging.EntryType.Warning)
        );
        assertTrue(
                l.logEx("test", "test message", new IOException("unit test"), Logging.EntryType.Error)
        );
        l.dispose();
        
        l = Logging.createInstance(core, "unit-test-"+Math.random()*10000, true, Logging.EntryType.getSet("Info"));
        assertTrue(
                l.logEx("test", "test message", new IOException("unit test"), Logging.EntryType.Info)
        );
        assertFalse(
                l.logEx("test", "test message", new IOException("unit test"), Logging.EntryType.Warning)
        );
        assertFalse(
                l.logEx("test", "test message", new IOException("unit test"), Logging.EntryType.Error)
        );
        l.dispose();
        
        l = Logging.createInstance(core, "unit-test-"+Math.random()*10000, true, Logging.EntryType.getSet("Warning"));
        assertFalse(
                l.logEx("test", "test message", new IOException("unit test"), Logging.EntryType.Info)
        );
        assertTrue(
                l.logEx("test", "test message", new IOException("unit test"), Logging.EntryType.Warning)
        );
        assertFalse(
                l.logEx("test", "test message", new IOException("unit test"), Logging.EntryType.Error)
        );
        l.dispose();
        
        l = Logging.createInstance(core, "unit-test-"+Math.random()*10000, true, Logging.EntryType.getSet("Error"));
        assertFalse(
                l.logEx("test", "test message", new IOException("unit test"), Logging.EntryType.Info)
        );
        assertFalse(
                l.logEx("test", "test message", new IOException("unit test"), Logging.EntryType.Warning)
        );
        assertTrue(
                l.logEx("test", "test message", new IOException("unit test"), Logging.EntryType.Error)
        );
        l.dispose();
    }
    /**
     * Tests {@link Logging#log(java.lang.String, java.lang.String, pals.base.Logging.EntryType)}
     * works.
     * 
     * @since 1.0
     */
    @Test
    public void testLog()
    {
        Logging l = Logging.createInstance(core, "unit-test-"+Math.random()*10000, true, null);
        assertTrue(
                l.log("test", "test message", Logging.EntryType.Info)
        );
        l.dispose();
    }
}
