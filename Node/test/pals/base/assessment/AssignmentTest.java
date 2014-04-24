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

import org.joda.time.DateTime;
import static org.junit.Assert.*;
import org.junit.Test;
import pals.TestWithCore;
import pals.base.database.Connector;

/**
 * Tests {@link Assignment}.
 * 
 * @version 1.0
 */
public class AssignmentTest extends TestWithCore
{
    /**
     * Tests accessors, mutators and construction.
     * 
     * @since 1.0
     */
    @Test
    public void testAccessorsMutators()
    {
        Connector conn = core.createConnector();
        
        Module m = new Module("test");
        m.persist(conn);
        
        Module m2 = new Module("test 2");
        m2.persist(conn);
        
        DateTime    dt = DateTime.now(),
                    dt2 = new DateTime(2014, 01, 12, 0, 0);
        
        Assignment ass = new Assignment(m, "ass 1", 15, true, 100, dt, true);
        
        assertEquals(-1, ass.getAssID());
        
        // Check constructor is correct
        assertEquals(m, ass.getModule());
        assertEquals("ass 1", ass.getTitle());
        assertEquals(15, ass.getWeight());
        assertEquals(true, ass.isActive());
        assertEquals(100, ass.getMaxAttempts());
        assertEquals(dt, ass.getDue());
        assertEquals(true, ass.isDueHandled());
        
        // Test mutators
        ass.setModule(m2);
        assertEquals(m2, ass.getModule());
        
        ass.setTitle("unit test assignment");
        assertEquals("unit test assignment", ass.getTitle());
        
        ass.setWeight(12345);
        assertEquals(12345, ass.getWeight());
        
        ass.setDue(dt2);
        assertEquals(dt2, ass.getDue());
        ass.setDue(null);
        assertNull(ass.getDue());
        
        ass.setDueHandled(false);
        assertFalse(ass.isDueHandled());
        ass.setDueHandled(true);
        assertTrue(ass.isDueHandled());
        
        DateTime    past = DateTime.now().minusDays(1),
                    future = DateTime.now().plusDays(1);
        
        ass.setDue(past);
        assertTrue(ass.isDueSurpassed());
        
        ass.setDue(future);
        assertFalse(ass.isDueSurpassed());
        
        // Dispose test data
        m.delete(conn);
        m2.delete(conn);
        conn.disconnect();
    }
    /**
     * Tests fetching assignments for a module. Also tests the equals method
     * of the model.
     * 
     * @since 1.0
     */
    @Test
    public void testLoadActive()
    {
        Connector conn = core.createConnector();
        
        Module m = new Module("unit test module");
        assertEquals(Module.PersistStatus.Success, m.persist(conn));
        
        Assignment assActive = new Assignment(m, "ass", 15, true, -1, null, false);
        assertEquals(Assignment.PersistStatus.Success, assActive.persist(conn));
        
        Assignment assNotActive = new Assignment(m, "ass not active", 15, false, -1, null, false);
        assertEquals(Assignment.PersistStatus.Success, assNotActive.persist(conn));
        
        Assignment[] asses = Assignment.loadActive(conn, m, true);
        assertEquals(1, asses.length);
        assertEquals(asses[0], assActive);
        
        asses = Assignment.loadActive(conn, m, false);
        assertEquals(1, asses.length);
        assertEquals(asses[0], assNotActive);
        
        // Delete assignments and test
        assActive.delete(conn);
        asses = Assignment.loadActive(conn, m, true);
        assertEquals(0, asses.length);
        
        assNotActive.delete(conn);
        asses = Assignment.loadActive(conn, m, false);
        assertEquals(0, asses.length);
        
        // Dispose test data
        m.delete(conn);
        conn.disconnect();
    }
    /**
     * Tests loading assignments for a module.
     * 
     * @since 1.0
     */
    @Test
    public void testLoadModule()
    {
        Connector conn = core.createConnector();
        
        Module m = new Module("unit test module");
        assertEquals(Module.PersistStatus.Success, m.persist(conn));
        
        Assignment ass1 = new Assignment(m, "ass", 15, true, -1, null, false);
        assertEquals(Assignment.PersistStatus.Success, ass1.persist(conn));
        
        Assignment ass2 = new Assignment(m, "ass 2", 15, false, -1, null, false);
        assertEquals(Assignment.PersistStatus.Success, ass2.persist(conn));
        
        Assignment[] asses = Assignment.load(conn, m, false);
        assertEquals(2, asses.length);
        
        asses = Assignment.load(conn, m, true);
        assertEquals(1, asses.length);
        assertEquals(asses[0], ass1);
        
        // Dispose test data
        ass1.delete(conn);
        ass2.delete(conn);
        m.delete(conn);
        conn.disconnect();
    }
    /**
     * Tests creating, loading and deleting assignments.
     * 
     * @since 1.0
     */
    @Test
    public void testCreateLoadDelete()
    {
        Connector conn = core.createConnector();
        // Create new test module
        Module m = new Module("unit test");
        // Create new model
        Assignment ass = new Assignment(null, "title", 15, true, -1, null, true);
        // Test different persistence checks
        assertEquals(Assignment.PersistStatus.Invalid_Module, ass.persist(conn));
        
        
        ass.setModule(m);
        assertEquals(Assignment.PersistStatus.Invalid_Module, ass.persist(conn));
        
        ass.setModule(null);
        assertEquals(Assignment.PersistStatus.Invalid_Module, ass.persist(conn));
        
        // Persist module and reset ready for later
        assertEquals(Module.PersistStatus.Success, m.persist(conn));
        ass.setModule(m);
        
        ass.setTitle(null);
        assertEquals(Assignment.PersistStatus.Invalid_Title, ass.persist(conn));
        
        ass.setTitle("test");
        
        ass.setWeight(-2);
        assertEquals(Assignment.PersistStatus.Invalid_Weight, ass.persist(conn));
        
        ass.setWeight(1);
        
        ass.setDue(DateTime.now().minusSeconds(1));
        assertEquals(Assignment.PersistStatus.Invalid_Due, ass.persist(conn));
        
        ass.setDue(null);
        // Persist, check ID
        assertEquals(Assignment.PersistStatus.Success, ass.persist(conn));
        // Reload model from ID - with and without module
        int id = ass.getAssID();
        assertTrue(id>=0);
        
        ass = Assignment.load(conn, m, id);
        assertNotNull(ass);
        
        ass = Assignment.load(conn, null, id);
        assertNotNull(ass);
        assertNotNull(ass.getModule());
        // Delete
        assertTrue(ass.delete(conn));
        assertEquals(-1, ass.getAssID());
        // Check it cannot be reloaded
        ass = Assignment.load(conn, null, id);
        assertNull(ass);
        // Dispose test data
        m.delete(conn);
        conn.disconnect();
    }
}
