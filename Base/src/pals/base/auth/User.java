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
 * 
 * @version 1.0
 */
public class User
{
    // Enums *******************************************************************
    /**
     * The status from persisting this model.
     * 
     * @since 1.0
     */
    public enum PersistStatus_User
    {
        /**
         * Successfully persisted.
         * 
         * @since 1.0
         */
        Success,
        /**
         * Failed to persist due to unknown state or exception.
         * 
         * @since 1.0
         */
        Failed,
        /**
         * Invalid username format.
         * 
         * @since 1.0
         */
        InvalidUsername_format,
        /**
         * Invalid username length.
         * 
         * @since 1.0
         */
        InvalidUsername_length,
        /**
         * Username already exists.
         * 
         * @since 1.0
         */
        InvalidUsername_exists,
        /**
         * Invalid password length.
         * 
         * @since 1.0
         */
        InvalidPassword_length,
        /**
         * Invalid e-mail format.
         * 
         * @since 1.0
         */
        InvalidEmail_format,
        /**
         * E-mail already in-use by another user.
         * 
         * @since 1.0
         */
        InvalidEmail_exists,
        /**
         * Invalid user-group.
         * 
         * @since 1.0
         */
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
    /**
     * Creates a new, unpersisted, instance of an abstract user.
     * 
     * @since 1.0
     */
    public User()
    {
        this(null, null, null, null, null);
    }
    /**
     * Creates a new, unpersisted, instance of an abstract user.
     * 
     * @param username The username/alias of the user.
     * @param password The password of the user; can be null if unused.
     * @param passwordSalt THe password-salt of the user; can be null if unused.
     * @param email The e-mail for the user; can be null if unused.
     * @param group The user-group of the user.
     * @since 1.0
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
     * @since 1.0
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
     * @since 1.0
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
     * @since 1.0
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
     * @since 1.0
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
     * @since 1.0
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
     * @since 1.0
     */
    public boolean delete(Connector conn)
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
     * Sets the username.
     * 
     * @param username The new username for the user; can only consist of
     * alpha-numeric characters.
     * @since 1.0
     */
    public void setUsername(String username)
    {
        this.username = username;
    }
    /**
     * Sets the password. This does not apply hashing; the authentication
     * plugin is expected to do this.
     * 
     * @param password The new password; can be null.
     * @since 1.0
     */
    public void setPassword(String password)
    {
        this.password = password;
    }
    /**
     * Sets the password salt.
     * 
     * @param passwordSalt The new password salt; can be null.
     * @since 1.0
     */
    public void setPasswordSalt(String passwordSalt)
    {
        this.passwordSalt = passwordSalt;
    }
    /**
     * Sets the e-mail.
     * 
     * @param email The new e-mail for the user.
     * @since 1.0
     */
    public void setEmail(String email)
    {
        this.email = email;
    }
    /**
     * Sets the user-group.
     * 
     * @param group The new user-group of the user.
     * @since 1.0
     */
    public void setGroup(UserGroup group)
    {
        this.group = group;
    }
    // Methods - Accessors *****************************************************
    /**
     * Indicates if the model has been persisted.
     * 
     * @return Indicates if the model has been persisted.
     * @since 1.0
     */
    public boolean isPersisted()
    {
        return userid != -1;
    }
    /**
     * Retrieves the user ID.
     * 
     * @return The user's identifier; -1 if the model has not been persisted.
     * @since 1.0
     */
    public int getUserID()
    {
        return userid;
    }
    /**
     * The username.
     * 
     * @return The user's username/alias; only alpha-numeric.
     * @since 1.0
     */
    public String getUsername()
    {
        return username;
    }
    /**
     * The password.
     * 
     * @return The user's password.
     * @since 1.0
     */
    public String getPassword()
    {
        return password;
    }
    /**
     * The password salt.
     * 
     * @return The user's password salt.
     * @since 1.0
     */
    public String getPasswordSalt()
    {
        return passwordSalt;
    }
    /**
     * The e-mail.
     * 
     * @return The user's e-mail address.
     * @since 1.0
     */
    public String getEmail()
    {
        return email;
    }
    /**
     * The user-group.
     * 
     * @return The class/user-group of the user.
     * @since 1.0
     */
    public UserGroup getGroup()
    {
        return group;
    }
    /**
     * The user's enrolled modules.
     * 
     * @param conn Database connector.
     * @return The modules the user is enrolled on.
     * @since 1.0
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
     * Minimum username length.
     * 
     * @return Minimum length of a username.
     * @since 1.0
     */
    public int getUsernameMin()
    {
        return 1;
    }
    /**
     * Maximum username length.
     * 
     * @return Maximum length of a username.
     * @since 1.0
     */
    public int getUsernameMax()
    {
        return 24;
    }
    /**
     * Minimum password length.
     * 
     * @return Minimum length of a password.
     * @since 1.0
     */
    public int getPasswordMin()
    {
        return 1;
    }
    /**
     * Maximum password length.
     * 
     * @return The maximum length of a password.
     * @since 1.0
     */
    public int getPasswordMax()
    {
        return 512;
    }
    /**
     * Minimum password salt length.
     * 
     * @return The minimum length of a password salt.
     * @since 1.0
     */
    public int getPasswordSaltMin()
    {
        return 0;
    }
    /**
     * Maximum password salt length.
     * 
     * @return The maximum length of a password.
     * @since 1.0
     */
    public int getPasswordSaltMax()
    {
        return 32;
    }
    /**
     * Minimum e-mail length.
     * 
     * @return The minimum length of an e-mail.
     * @since 1.0
     */
    public int getEmailMin()
    {
        return 1;
    }
    /**
     * Maximum e-mail length.
     * 
     * @return The maximum length of an e-mail.
     * @since 1.0
     */
    public int getEmailMax()
    {
        return 128;
    }

    // Methods - Overrides *****************************************************
    /**
     * Checks a specified object is equal to the current instance, based
     * on being the same type and having the same identifier.
     * 
     * @param o The object being compared.
     * @return True = same, false = not same.
     * @since 1.0
     */
    @Override
    public boolean equals(Object o)
    {
        if(o == null || !(o instanceof User))
            return false;
        User a = (User)o;
        return a.userid == userid;
    }
    /**
     * The hash-code based on the group identifier.
     * 
     * @return The hash-code.
     * @since 1.0
     */
    @Override
    public int hashCode()
    {
        return group != null ? group.getGroupID() : -1;
    }
}
