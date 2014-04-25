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

import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;
import pals.TestWithCore;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;

/**
 * Tests {@link DatabaseHttpSession}.
 * 
 * @version 1.0
 */
public class DatabaseHttpSessionTest extends TestWithCore
{
    /**
     * Tests creating, loading and deleting a session, as well as the mutators
     * and accessors.
     * 
     * @throws DatabaseException Refer to class for reason.
     * @throws ClassNotFoundException Refer to class for reason.
     * @throws IOException Refer to class for reason.
     * @throws IllegalArgumentException Refer to class for reason.
     * @since 1.0
     */
    @Test
    public void testCreateLoadDeleteMutatorsAccessors() throws DatabaseException, ClassNotFoundException, IOException, IllegalArgumentException
    {
        Connector conn = core.createConnector();
        
        // Create
        DatabaseHttpSession sess = DatabaseHttpSession.load(conn, null, "127.0.0.1");
        assertNotNull(sess);
        
        assertTrue(sess.isEmpty());
        sess.setAttribute("a", 123);
        sess.setAttribute("b", "test");
        assertFalse(sess.isEmpty());
        
        assertTrue(sess.contains("a"));
        assertTrue(sess.contains("b"));
        assertFalse(sess.contains("dsadas"));
        // Persist
        sess.persist(conn);
        
        // Load
        String base64id = sess.getIdBase64();
        sess = DatabaseHttpSession.load(conn, base64id, "127.0.0.1");
        assertNotNull(sess);
        assertEquals(base64id, sess.getIdBase64());
        
        assertEquals(2, sess.size());
        assertEquals(123, sess.getAttribute("a"));
        assertEquals("test", sess.getAttribute("b"));
        
        // Delete
        sess.destroy(conn);
        
        // Reload
        sess = DatabaseHttpSession.load(conn, base64id, "127.0.0.1");
        assertNotNull(sess);
        assertNotEquals(base64id, sess.getIdBase64());
        
        conn.disconnect();
    }
}
