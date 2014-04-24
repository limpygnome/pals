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
package pals.base.assessment;

import static org.junit.Assert.*;
import org.junit.Test;
import pals.TestWithCore;
import pals.base.Plugin;
import pals.base.UUID;
import pals.base.database.Connector;

/**
 * Tests {@link TypeQuestion}.
 * 
 * @version 1.0
 */
public class TypeQuestionTest extends TestWithCore
{
    @Test
    public void testMutatorsAccessors()
    {
        UUID    uuidTQ = UUID.generateVersion4(),
                uuidP = UUID.generateVersion4();
        
        TypeQuestion tq = new TypeQuestion(uuidTQ, uuidP, "title", "description");
        
        // Test constructor
        assertEquals(uuidTQ, tq.getUuidQType());
        assertEquals(uuidP, tq.getUuidPlugin());
        assertEquals("title", tq.getTitle());
        assertEquals("description", tq.getDescription());
        
        // Test new values
        uuidTQ = UUID.generateVersion4();
        uuidP = UUID.generateVersion4();
        
        tq.setUuidQType(uuidTQ);
        tq.setUuidPlugin(uuidP);
        tq.setTitle("title 2");
        tq.setDescription("description 2");
        
        assertEquals(uuidTQ, tq.getUuidQType());
        assertEquals(uuidP, tq.getUuidPlugin());
        assertEquals("title 2", tq.getTitle());
        assertEquals("description 2", tq.getDescription());
    }
    @Test
    public void testCreateLoadDelete()
    {
        Plugin  p = core.getPlugins().getPlugins()[0];
        UUID    uuidP = p.getUUID(),
                uuidTQ = UUID.generateVersion4();
        Connector conn = core.createConnector();
        
        // Create
        TypeQuestion tq = new TypeQuestion(uuidTQ, uuidP, "title", "description");
        assertEquals(TypeQuestion.PersistStatus.Success, tq.persist(conn));
        
        // Load
        tq = TypeQuestion.load(conn, uuidTQ);
        assertNotNull(tq);
        
        assertEquals(uuidTQ, tq.getUuidQType());
        assertEquals(uuidP, tq.getUuidPlugin());
        assertEquals("title", tq.getTitle());
        assertEquals("description", tq.getDescription());
        
        // Delete
        assertTrue(tq.delete(conn));
        
        tq = TypeQuestion.load(conn, uuidTQ);
        assertNull(tq);
        
        // Register
        tq = TypeQuestion.register(conn, core, p, uuidTQ, "title", "description");
        assertNotNull(tq);
        
        // Load
        tq = TypeQuestion.load(conn, uuidTQ);
        assertNotNull(tq);
        
        // Unregister
        assertTrue(TypeQuestion.unregister(conn, uuidTQ));
        tq = TypeQuestion.load(conn, uuidTQ);
        assertNull(tq);
        
        conn.disconnect();
    }
}
