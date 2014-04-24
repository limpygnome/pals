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

import org.junit.Test;
import pals.base.UUID;
import static org.junit.Assert.*;
import pals.TestWithCore;
import pals.base.Plugin;
import pals.base.database.Connector;

/**
 * Tests {@link TypeCriteria}.
 * 
 * @version 1.0
 */
public class TypeCriteriaTest extends TestWithCore
{
    @Test
    public void testMutatorsAccessors()
    {
        UUID    uuidTC = UUID.generateVersion4(),
                uuidP = UUID.generateVersion4();
        
        TypeCriteria tc = new TypeCriteria(uuidTC, uuidP, "title", "description");
        
        // Test constructor
        assertEquals(uuidTC, tc.getUuidCType());
        assertEquals(uuidP, tc.getUuidPlugin());
        assertEquals("title", tc.getTitle());
        assertEquals("description", tc.getDescription());
        
        // Test new values
        uuidTC = UUID.generateVersion4();
        uuidP = UUID.generateVersion4();
        
        tc.setUuidCType(uuidTC);
        tc.setUuidPlugin(uuidP);
        tc.setTitle("title 2");
        tc.setDescription("description 2");
        
        assertEquals(uuidTC, tc.getUuidCType());
        assertEquals(uuidP, tc.getUuidPlugin());
        assertEquals("title 2", tc.getTitle());
        assertEquals("description 2", tc.getDescription());
    }
    @Test
    public void testCreateLoadDelete()
    {
        Plugin  p = core.getPlugins().getPlugins()[0];
        UUID    uuidP = p.getUUID(),
                uuidTC = UUID.generateVersion4();
        Connector conn = core.createConnector();
        
        // Create
        TypeCriteria tc = new TypeCriteria(uuidTC, uuidP, "title", "description");
        assertEquals(TypeCriteria.PersistStatus.Success, tc.persist(conn));
        
        // Load
        tc = TypeCriteria.load(conn, uuidTC);
        assertNotNull(tc);
        
        assertEquals(uuidTC, tc.getUuidCType());
        assertEquals(uuidP, tc.getUuidPlugin());
        assertEquals("title", tc.getTitle());
        assertEquals("description", tc.getDescription());
        
        // Delete
        assertTrue(tc.delete(conn));
        
        tc = TypeCriteria.load(conn, uuidTC);
        assertNull(tc);
        
        // Register
        tc = TypeCriteria.register(conn, core, p, uuidTC, "title", "description");
        assertNotNull(tc);
        
        // Load
        tc = TypeCriteria.load(conn, uuidTC);
        assertNotNull(tc);
        
        // Unregister
        assertTrue(TypeCriteria.unregister(conn, uuidTC));
        tc = TypeCriteria.load(conn, uuidTC);
        assertNull(tc);
        
        conn.disconnect();
    }
}
