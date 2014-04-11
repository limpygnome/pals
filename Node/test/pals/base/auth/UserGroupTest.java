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
package pals.base.auth;

import static org.junit.Assert.*;
import org.junit.Test;
import pals.TestWithCore;
import pals.base.database.Connector;

/**
 * Tests {@link UserGroup}.
 * 
 * @version 1.0
 */
public class UserGroupTest extends TestWithCore
{
    /**
     * Tests creating, fetching and removing a group.
     * 
     * @since 1.0
     */
    @Test
    public void testGroupCreateFetchRemove()
    {
        Connector conn = core.createConnector();
        UserGroup ug;
        
        // Create group with invalid title
        ug = new UserGroup(null, false, false, false, false, false, false);
        assertEquals(UserGroup.PersistStatus_UserGroup.Title_Length, ug.persist(conn));
        
        // Update title to be valid
        ug.setTitle("unit test group");
        assertEquals(UserGroup.PersistStatus_UserGroup.Success, ug.persist(conn));
        
        // Check we can fetch the group
        int groupid = ug.getGroupID();
        ug = null;
        
        ug = UserGroup.load(conn, groupid);
        assertNotNull(ug);
        
        assertEquals(groupid, ug.getGroupID());
        assertTrue(ug.getGroupID() >= 0);
        
        // Remove the group
        assertTrue(ug.remove(conn));
        
        // Check the group is gone
        ug = UserGroup.load(conn, groupid);
        assertNull(ug);
        
        conn.disconnect();
    }
    
    /**
     * Tests the accessors of a group.
     * 
     * @since 1.0
     */
    @Test
    public void testGroupAccessors()
    {
        UserGroup ug = new UserGroup("test", true, true, true, true, true, true);
        
        // Test constructor values
        assertEquals("test", ug.getTitle());
        assertEquals(true, ug.isAdmin());
        assertEquals(true, ug.isAdminModules());
        assertEquals(true, ug.isAdminQuestions());
        assertEquals(true, ug.isAdminSystem());
        assertEquals(true, ug.isAdminUsers());
        assertEquals(true, ug.isMarkerGeneral());
        assertEquals(true, ug.isUserLogin());
        
        // Test mutators/accessors
        ug.setTitle("unit testing");
        assertEquals("unit testing", ug.getTitle());
        
        ug.setAdminModules(false);
        assertEquals(false, ug.isAdminModules());
        assertEquals(true, ug.isAdmin());
        
        ug.setAdminQuestions(false);
        assertEquals(false, ug.isAdminQuestions());
        assertEquals(true, ug.isAdmin());
        
        ug.setAdminSystem(false);
        assertEquals(false, ug.isAdminSystem());
        assertEquals(true, ug.isAdmin());

        ug.setAdminUsers(false);
        assertEquals(false, ug.isAdminUsers());
        
        // Test is admin - should all be false
        assertEquals(false, ug.isAdmin());
        
        // Continue...
        ug.setMarkerGeneral(false);
        assertEquals(false, ug.isMarkerGeneral());
        
        ug.setUserLogin(false);
        assertEquals(false, ug.isUserLogin());
    }
}
