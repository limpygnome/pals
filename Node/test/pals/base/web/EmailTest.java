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
import org.joda.time.DateTime;
import org.junit.Test;
import pals.TestWithCore;
import pals.base.database.Connector;

/**
 * Tests {@link Email}.
 * 
 * @version 1.0
 */
public class EmailTest extends TestWithCore
{
    @Test
    public void testMutatorsAccessors()
    {
        DateTime la = DateTime.now();
        Email e = new Email("title", "content", "dest@dest.com", la, 123);
        
        // Test constructor
        assertEquals("title", e.getTitle());
        assertEquals("content", e.getContent());
        assertEquals("dest@dest.com", e.getDestination());
        assertEquals(la, e.getLastAttempted());
        assertEquals(123, e.getAttempts());
        
        // Test mutators
        e.setTitle("test title");
        assertEquals("test title", e.getTitle());
        
        e.setContent("test content");
        assertEquals("test content", e.getContent());
        
        e.setDestination("admin@localhost.com");
        assertEquals("admin@localhost.com", e.getDestination());
        
        la = DateTime.now().minusDays(2);
        e.setLastAttempted(la);
        assertEquals(la, e.getLastAttempted());
        
        e.setAttempts(1);
        assertEquals(1, e.getAttempts());
        
        e.setAttemptsIncrement();
        assertEquals(2, e.getAttempts());
        
        e.setAttemptsIncrement();
        assertEquals(3, e.getAttempts());
    }
    
    @Test
    public void testCreateLoadDelete()
    {
        Connector conn = core.createConnector();
        
        // Create
        DateTime la = DateTime.now();
        Email e = new Email("title", "content", "dest@dest.com", la, 1);
        assertTrue(e.persist(conn));
        
        // Load
        int emailid = e.getEmailID();
        e = Email.load(conn, emailid);
        assertNotNull(e);
        assertEquals(emailid, e.getEmailID());
        assertEquals("title", e.getTitle());
        assertEquals("content", e.getContent());
        assertEquals("dest@dest.com", e.getDestination());
        assertEquals(la, e.getLastAttempted());
        assertEquals(1, e.getAttempts());
        
        // Delete
        assertTrue(e.delete(conn));
        
        // Reload
        e = Email.load(conn, emailid);
        assertNull(e);
        
        // Test persisting and deleting multiple
        e = new Email("title", "content", "dest@dest.com", la, 1);
        assertTrue(e.persist(conn));
        e = new Email("title", "content", "dest@dest.com", la, 1);
        assertTrue(e.persist(conn));
        e = new Email("title", "content", "dest@dest.com", la, 1);
        assertTrue(e.persist(conn));
        
        assertEquals(3, Email.load(conn, 1000, 0).length);
        
        assertTrue(Email.deleteAll(conn));
        
        assertEquals(0, Email.load(conn, 1000, 0).length);
        
        conn.disconnect();
    }
}
