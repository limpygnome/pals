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
import pals.base.auth.User;
import pals.base.auth.UserGroup;
import pals.base.database.Connector;

/**
 * Tests {@link InstanceAssignment}.
 * 
 * @version 1.0
 */
public class InstanceAssignmentTest extends TestWithCore
{
    /**
     * Tests the mutators, accessors, creating/persisting, loading and
     * deleting.
     * 
     * @since 1.0
     */
    @Test
    public void testMutatorsAccessorsCreateLoadDelete()
    {
        Connector conn = core.createConnector();
        // Create test data
        User u = new User("user1", null, null, "user1@user1.com", UserGroup.load(conn, 1));
        assertEquals(User.PersistStatus_User.Success, u.persist(core, conn));
        Module m = new Module("test module");
        assertEquals(Module.PersistStatus.Success, m.persist(conn));
        Assignment ass = new Assignment(m, "title", 100, false, -1, null, true);
        assertEquals(Assignment.PersistStatus.Success, ass.persist(conn));
        
        DateTime    start = DateTime.now().minusDays(1),
                    end = DateTime.now();
        
        // Test constructor
        InstanceAssignment ia = new InstanceAssignment(u, ass, InstanceAssignment.Status.Marking, start, end, 100.0);
        
        assertEquals(u, ia.getUser());
        assertEquals(ass, ia.getAss());
        assertEquals(InstanceAssignment.Status.Marking, ia.getStatus());
        assertEquals(start, ia.getTimeStart());
        assertEquals(end, ia.getTimeEnd());
        assertEquals(100.0, ia.getMark(), 0.0);
        
        // Test mutators
        ia.setUser(null);
        assertNull(ia.getUser());
        ia.setUser(u);
        assertEquals(u, ia.getUser());
        
        ia.setAss(null);
        assertNull(ia.getAss());
        ia.setAss(ass);
        assertEquals(ass, ia.getAss());
        
        assertEquals(InstanceAssignment.Status.Marking, ia.getStatus());
        ia.setStatus(InstanceAssignment.Status.Active);
        assertEquals(InstanceAssignment.Status.Active, ia.getStatus());
        
        ia.setMark(123.0);
        assertEquals(123.0, ia.getMark(), 0.0);
        
        start = DateTime.now().minusDays(2);
        ia.setTimeStart(start);
        assertEquals(start, ia.getTimeStart());
        
        end = DateTime.now().minusDays(1);
        ia.setTimeEnd(end);
        assertEquals(end, ia.getTimeEnd());
        
        // Persist
        ia.setUser(null);
        assertEquals(InstanceAssignment.PersistStatus.Invalid_User, ia.persist(conn));
        ia.setUser(u);
        
        ia.setAss(null);
        assertEquals(InstanceAssignment.PersistStatus.Invalid_Assignment, ia.persist(conn));
        ia.setAss(ass);
        
        ia.setMark(-1.0);
        assertEquals(InstanceAssignment.PersistStatus.Invalid_Mark, ia.persist(conn));
        ia.setMark(100.0);
        
        ia.setUser(null);
        assertEquals(InstanceAssignment.PersistStatus.Invalid_User, ia.persist(conn));
        ia.setUser(u);
        
        assertEquals(InstanceAssignment.PersistStatus.Success, ia.persist(conn));
        
        // Load
        int aiid = ia.getAIID();
        ia = InstanceAssignment.load(conn, null, null, aiid);
        assertNotNull(ia);
        
        assertEquals(u, ia.getUser());
        assertEquals(ass, ia.getAss());
        assertEquals(InstanceAssignment.Status.Active, ia.getStatus());
        assertEquals(100.0, ia.getMark(), 0.0);
        assertEquals(start, ia.getTimeStart());
        assertEquals(end, ia.getTimeEnd());
        
        // Delete
        assertTrue(ia.delete(conn));
        
        // Reload
        ia = InstanceAssignment.load(conn, null, null, aiid);
        assertNull(ia);
        
        // Dispose test data
        assertTrue(ass.delete(conn));
        assertTrue(m.delete(conn));
        assertTrue(u.delete(conn));
        
        conn.disconnect();
    }
}
