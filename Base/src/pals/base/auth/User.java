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
    Version:    1.0
    Authors:    Marcus Craske           <limpygnome@gmail.com>
    ----------------------------------------------------------------------------
*/
package pals.base.auth;

import pals.base.Logging;
import pals.base.NodeCore;
import pals.base.assessment.Module;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.database.Result;

/**
 * An abstract user of the system.
 * 
 * The idea is for all authentication plugins to use a single user model from
 * the base (this class) to avoid a lack of third-party plugin support.
 * Therefore the password, password-salt and e-mail fields in this model are
 * completely optional and may need to be validated, from an input source
 * (such as a web-interface), manually.
 * 
 * Therefore if you write your own authentication manager, ensure the
 * password/password-salt/e-mail fields are valid! The password should also
 * be salted with the salt provided, hashing is not performed by this model
 * (which simply acts as a data container).
 */
public class User
{
    // Enums *******************************************************************
    /**
     * The status from persisting this model.
     */
    public enum PersistStatus_User
    {
        Success,
        Failed,
        InvalidUsername_format,
        InvalidUsername_length,
        InvalidUsername_exists,
        InvalidPassword_length,
        InvalidEmail_format,
        InvalidEmail_exists,
        InvalidGroup
    }
    // Fields ******************************************************************
    private int         userid;         // The unique identifier of the user.
    private String      username;       // The username/alias of the user.
    private String      password;       // The password for default authentication.
    private String      passwordSalt;   // The password salt used for the password.
    private String      email;          // The user's e-mail.
    private UserGroup   group;          // The class/group of the user.
    // Fields - Caching ********************************************************
    private Module[] modules;           // The modules a user belongs to.
    // Methods - Constructors **************************************************
    public User()
    {
        this(null, null, null, null, null);
    }
    /**
     * Creates a new instance of an abstract user.
     * 
     * @param username The username/alias of the user.
     * @param password The password of the user; can be null if unused.
     * @param passwordSalt THe password-salt of the user; can be null if unused.
     * @param email The e-mail for the user; can be null if unused.
     * @param group The user-group of the user.
     */
    public User(String username, String password, String passwordSalt, String email, UserGroup group)
    {
        this.userid = -1;
        this.username = username;
        this.password = password;
        this.passwordSalt = passwordSalt;
        this.email = email;
        this.group = group;
    }
    // Methods - Database Persistence ******************************************
    /**
     * Loads a user from a username.
     * 
     * @param conn Database connector.
     * @param username Username of user.
     * @return Instance of user or null.
     */
    public static User load(Connector conn, String username)
    {
        try
        {
            Result res = conn.read("SELECT * FROM pals_users WHERE username=?;", username);
            return res.next() ? load(conn, res) : null;
        }
        catch(DatabaseException ex)
        {
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return null;
        }
    }
    /**
     * Loads a user from an email.
     * 
     * @param conn Database connector.
     * @param email The e-mail of the user.
     * @return Instance of user or null.
     */
    public static User loadByEmail(Connector conn, String email)
    {
        try
        {
            Result res = conn.read("SELECT * FROM pals_users WHERE email=?;", email);
            return res.next() ? load(conn, res) : null;
        }
        catch(DatabaseException ex)
        {
            return null;
        }
    }
    /**
     * Loads a user from a userid.
     * 
     * @param conn Database connector.
     * @param userid The userid of the user to load.
     * @return Instance of user or null.
     */
    public static User load(Connector conn, int userid)
    {
        try
        {
            Result res = conn.read("SELECT * FROM pals_users WHERE userid=?;", userid);
            return res.next() ? load(conn, res) : null;
        }
        catch(DatabaseException ex)
        {
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return null;
        }
    }
    /**
     * Loads a user from a result, as well as loading the user-group from a
     * separate query.
     * 
     * @param conn Database connector.
     * @param result Query result.
     * @return Instance of user or null.
     */
    public static User load(Connector conn, Result result)
    {
        
        try
        {
            // Load the user-group
            UserGroup ug = UserGroup.load(conn, (int)result.get("groupid"));
            if(ug == null)
                return null;
            // Load and return new instance
            User user = new User(
                    (String)result.get("username"),
                    (String)result.get("password"),
                    (String)result.get("password_salt"),
                    (String)result.get("email"),
                    ug
                    );
            user.userid = (int)result.get("userid");
            return user;
        }
        catch(DatabaseException ex)
        {
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return null;
        }
    }
    /**
     * Persists the data. If the model has not been persisted, the userid from
     * creating the model is assigned to userid (if the operation is
     * successful).
     * 
     * Note:
     * - The password minimum length is only checked if non-null; therefore
     *   a password is optional.
     * - A password salt is also optional.
     * - An e-mail address is optional.
     * 
     * @param core The current instance of the core.
     * @param conn Database connector.
     * @return Status of the operation.
     */
    public PersistStatus_User persist(NodeCore core, Connector conn)
    {
        // Validate fields
        if(username == null || username.length() < getUsernameMin() || username.length() > getUsernameMax())
            return PersistStatus_User.InvalidUsername_length;
        else if(!username.matches("^[a-zA-Z0-9]+$"))
            return PersistStatus_User.InvalidUsername_format;
        else if(password != null && (password.length() < getPasswordMin()))
            return PersistStatus_User.InvalidPassword_length;
        // Regex from: http://www.regular-expressions.info/email.html
        else if(email != null && (email.length() < getEmailMin() || email.length() > getEmailMax() || !email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}$")))
            return PersistStatus_User.InvalidEmail_format;
        else if(group == null || group.getGroupID() == -1)
            return PersistStatus_User.InvalidGroup;
        try
        {
            // Validate uniqueness
            if(email != null && (long)conn.executeScalar("SELECT COUNT('') FROM pals_users WHERE email=? AND NOT userid=?;", email, userid) > 0)
                return PersistStatus_User.InvalidEmail_exists;
            else if((long)conn.executeScalar("SELECT COUNT('') FROM pals_users WHERE username=? AND NOT userid=?;", username, userid) > 0)
                return PersistStatus_User.InvalidUsername_exists;
            // Persist the data
            if(userid == -1)
            {
                userid = (int)conn.executeScalar("INSERT INTO pals_users (username, password, password_salt, email, groupid) VALUES (?,?,?,?,?) "
                        + "RETURNING userid;",
                        username,
                        password,
                        passwordSalt,
                        email,
                        group.getGroupID()
                        );
            }
            else
            {
                conn.execute("UPDATE pals_users SET username=?, password=?, password_salt=?, email=?, groupid=? WHERE userid=?",
                        username,
                        password,
                        passwordSalt,
                        email,
                        group.getGroupID(),
                        userid
                        );
            }
            return PersistStatus_User.Success;
        }
        catch(DatabaseException ex)
        {
            core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return PersistStatus_User.Failed;
        }
    }
    /**
     * Unpersists the current model.
     * 
     * @param conn Database connector.
     * @return Indicates if the operation succeeded.
     */
    public boolean remove(Connector conn)
    {
        try
        {
            if(userid != -1)
            {
                conn.execute("DELETE FROM pals_users WHERE userid=?;", userid);
                userid = -1;
                return true;
            }
        }
        catch(DatabaseException ex)
        {
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
        }
        return false;
    }
    // Methods - Mutators ******************************************************
    /**
     * @param username The new username for the user; can only consist of
     * alpha-numeric characters.
     */
    public void setUsername(String username)
    {
        this.username = username;
    }
    /**
     * @param password The new password; can be null.
     */
    public void setPassword(String password)
    {
        this.password = password;
    }
    /**
     * @param passwordSalt The new password salt; can be null.
     */
    public void setPasswordSalt(String passwordSalt)
    {
        this.passwordSalt = passwordSalt;
    }
    /**
     * @param email The new e-mail for the user.
     */
    public void setEmail(String email)
    {
        this.email = email;
    }
    /**
     * @param group The new user-group of the user.
     */
    public void setGroup(UserGroup group)
    {
        this.group = group;
    }
    // Methods - Accessors *****************************************************
    /**
     * @return Indicates if the model has been persisted.
     */
    public boolean isPersisted()
    {
        return userid != -1;
    }
    /**
     * @return The user's identifier; -1 if the model has not been persisted.
     */
    public int getUserID()
    {
        return userid;
    }
    /**
     * @return The user's username/alias; only alpha-numeric.
     */
    public String getUsername()
    {
        return username;
    }
    /**
     * @return The user's password.
     */
    public String getPassword()
    {
        return password;
    }
    /**
     * @return The user's password salt.
     */
    public String getPasswordSalt()
    {
        return passwordSalt;
    }
    /**
     * @return The user's e-mail address.
     */
    public String getEmail()
    {
        return email;
    }
    /**
     * @return The class/user-group of the user.
     */
    public UserGroup getGroup()
    {
        return group;
    }
    /**
     * @param conn Database connector.
     * @return The modules the user is enrolled on.
     */
    public Module[] getModules(Connector conn)
    {
        // Check if the user's modules have been loaded, else cache them
        // -- Expensive operation
        if(modules == null)
            modules = Module.load(conn, this);
        return modules;
    }
    // Methods - Accessors - Limits ********************************************
    /**
     * @return Minimum length of a username.
     */
    public int getUsernameMin()
    {
        return 1;
    }
    /**
     * @return Maximum length of a username.
     */
    public int getUsernameMax()
    {
        return 24;
    }
    /**
     * @return Minimum length of a password.
     */
    public int getPasswordMin()
    {
        return 1;
    }
    /**
     * @return The maximum length of a password.
     */
    public int getPasswordMax()
    {
        return 24;
    }
    /**
     * @return The minimum length of a password salt.
     */
    public int getPasswordSaltMin()
    {
        return 0;
    }
    /**
     * @return The maximum length of a password.
     */
    public int getPasswordSaltMax()
    {
        return 32;
    }
    /**
     * @return The minimum length of an e-mail.
     */
    public int getEmailMin()
    {
        return 1;
    }
    /**
     * @return The maximum length of an e-mail.
     */
    public int getEmailMax()
    {
        return 128;
    }
}
