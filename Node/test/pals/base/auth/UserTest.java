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
 * Tests {@link User}.
 * 
 * @version 1.0
 */
public class UserTest extends TestWithCore
{
    /**
     * Tests creating/fetching/removing users. This does not test password
     * and password salt, since these are optional fields handled by
     * an authentication plugin.
     * 
     * @since 1.0
     */
    @Test
    public void testUserCreateFetchRemove()
    {
        Connector conn = core.createConnector();
        
        // Load user-group - default DB installation contains user-groups
        UserGroup ug = UserGroup.load(conn, 1);
        assertNotNull(ug);
        
        User u;
        
        // Add and remove valid user
        u = new User("username", "password", "password salt", "email@email.com", ug);
        assertEquals(User.PersistStatus_User.Success, u.persist(core, conn));
        
        int userid = u.getUserID();
        assertTrue(userid >= 0);
        u = null;
        
        u = User.load(conn, userid);
        assertNotNull(u);
        assertTrue(u.remove(conn));
        
        u = User.load(conn, userid);
        assertNull(u);
        
        // Test invalid values for registration
        // -- Invalid username length
        assertEquals(User.PersistStatus_User.InvalidUsername_length, new User("", "password", "password salt", "email@email.com", ug).persist(core, conn));
        // -- Invalid username format
        assertEquals(User.PersistStatus_User.InvalidUsername_format, new User("spac es", "password", "password salt", "email@email.com", ug).persist(core, conn));
        // -- Invalid email
        assertEquals(User.PersistStatus_User.InvalidEmail_format, new User("username", "password", "password salt", "email222222", ug).persist(core, conn));
        // -- Invalid - email and username exists
        u = new User("username", "password", "password salt", "email@email.com", ug);
        assertEquals(User.PersistStatus_User.Success, u.persist(core, conn));
        
        assertEquals(User.PersistStatus_User.InvalidEmail_exists, new User("username2", "password", "password salt", "email@email.com", ug).persist(core, conn));
        assertEquals(User.PersistStatus_User.InvalidUsername_exists, new User("username", "password", "password salt", "email2@email.com", ug).persist(core, conn));
        
        userid = u.getUserID();
        assertTrue(u.remove(conn));
        assertTrue(userid >= 0);
        
        assertNull(User.load(conn, userid));
    }
    @Test
    public void testUserAccessors()
    {
        Connector conn = core.createConnector();
        
        UserGroup ug = UserGroup.load(conn, 1);
        assertNotNull(ug);
        
        User u = new User("username", "password", "password salt", "email", ug);
        
        // Test constructor/accessors
        assertEquals("username", u.getUsername());
        assertEquals("password", u.getPassword());
        assertEquals("password salt", u.getPasswordSalt());
        assertEquals("email", u.getEmail());
        assertEquals(ug, u.getGroup());
        
        // Test mutators/accessors
        u.setUsername("username2");
        assertEquals("username2", u.getUsername());
        
        u.setPassword("pass");
        assertEquals("pass", u.getPassword());
        
        u.setPasswordSalt("salt");
        assertEquals("salt", u.getPasswordSalt());
        
        u.setEmail("email2");
        assertEquals("email2", u.getEmail());
        
        ug = new UserGroup("unit-testing", true, true, true, true, true, true);
        u.setGroup(ug);
        assertEquals(ug, u.getGroup());
    }
}
