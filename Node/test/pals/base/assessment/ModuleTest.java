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
import pals.base.auth.User;
import pals.base.auth.UserGroup;
import pals.base.database.Connector;

/**
 * Tests {@link Module}.
 * 
 * @version 1.0
 */
public class ModuleTest extends TestWithCore
{
    /**
     * Tests mutators and accessors.
     * 
     * @since 1.0
     */
    @Test
    public void testMutatorsAccessors()
    {
        Module m = new Module("test title");
        
        assertEquals(-1, m.getModuleID());
        
        // Test constructor
        assertEquals("test title", m.getTitle());
        
        // Test mutators
        m.setTitle("title");
        assertEquals("title", m.getTitle());
        
        // Min/max checking
        assertTrue(m.getTitleMin() < m.getTitleMax());
        assertTrue(m.getTitleMin() > 0);
    }
    /**
     * Tests creating/persisting, loading and deleting assignments.
     * 
     * @since 1.0
     */
    @Test
    public void testCreateLoadDelete()
    {
        Connector conn = core.createConnector();
        Module m = new Module(null);
        
        // Invalid title
        assertEquals(Module.PersistStatus.Failed_title_length, m.persist(conn));
        
        // Correct title
        m.setTitle("test module");
        assertEquals(Module.PersistStatus.Success, m.persist(conn));
        
        // Reload model
        int moduleid = m.getModuleID();
        m = Module.load(conn, moduleid);
        assertNotNull(m);
        assertEquals("test module", m.getTitle());
        assertTrue(m.getModuleID() >= 0);
        assertEquals(moduleid, m.getModuleID());
        
        conn.disconnect();
    }
    /**
     * Tests enrollment and assignments.
     * 
     * @since 1.0
     */
    @Test
    public void testEnrollmentAssignments()
    {
        Connector conn = core.createConnector();
        
        // Create module and users
        Module m = new Module("test module");
        assertEquals(Module.PersistStatus.Success, m.persist(conn));
        
        UserGroup ug = UserGroup.load(conn, 1);
        User    stu1 = new User("usernam1", null, null, "stu1@test.com", ug),
                stu2 = new User("usernam2", null, null, "stu2@test.com", ug),
                stu3 = new User("usernam3", null, null, "stu3@test.com", ug);
        
        assertEquals(User.PersistStatus_User.Success, stu1.persist(core, conn));
        assertEquals(User.PersistStatus_User.Success, stu2.persist(core, conn));
        assertEquals(User.PersistStatus_User.Success, stu3.persist(core, conn));
        
        // Test adding one user
        assertTrue(m.usersAdd(conn, stu1));
        assertTrue(m.isEnrolled(conn, stu1));
        assertEquals(1, m.getUsersEnrolled(conn));
        
        // Test adding all users
        assertTrue(m.usersAdd(conn, stu2));
        assertTrue(m.isEnrolled(conn, stu2));
        
        assertTrue(m.usersAdd(conn, stu3));
        assertTrue(m.isEnrolled(conn, stu3));
        
        assertEquals(3, m.getUsersEnrolled(conn));
        
        // Test removing a user
        assertTrue(m.usersRemove(conn, stu1));
        assertFalse(m.isEnrolled(conn, stu1));
        assertEquals(2, m.getUsersEnrolled(conn));
        
        // Test removing all users
        m.usersRemoveAll(conn);
        assertFalse(m.isEnrolled(conn, stu1));
        assertFalse(m.isEnrolled(conn, stu2));
        assertFalse(m.isEnrolled(conn, stu3));
        assertEquals(0, m.getUsersEnrolled(conn));
        
        // Test assignment counter
        assertEquals(0, m.getTotalAssignments(conn));
        
        Assignment a = new Assignment(m, "unit test", 100, false, -1, null, true);
        assertEquals(Assignment.PersistStatus.Success, a.persist(conn));
        assertEquals(1, m.getTotalAssignments(conn));
        
        assertTrue(a.delete(conn));
        assertEquals(0, m.getTotalAssignments(conn));
        
        // Dispose data
        assertTrue(stu1.delete(conn));
        assertTrue(stu2.delete(conn));
        assertTrue(stu3.delete(conn));
        
        m.delete(conn);
        
        conn.disconnect();
    }
}
