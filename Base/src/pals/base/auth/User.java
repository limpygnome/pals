package pals.base.auth;

import pals.base.Logging;
import pals.base.NodeCore;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.database.Result;

/**
 * An abstract user of the system.
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
    // Methods - Constructors **************************************************
    private User(int userid, String username, String password, String passwordSalt, String email, UserGroup group)
    {
        this.userid = userid;
        this.username = username;
        this.password = password;
        this.passwordSalt = passwordSalt;
        this.email = email;
        this.group = group;
    }
    // Methods - Database Persistence ******************************************
    /**
     * @return Creates a new, unpersisted, user.
     */
    public static User create()
    {
        return new User(-1, null, null, null, null, null);
    }
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
            return new User(
                    (int)result.get("userid"),
                    (String)result.get("username"),
                    (String)result.get("password"),
                    (String)result.get("password_salt"),
                    (String)result.get("email"),
                    ug
                    );
        }
        catch(DatabaseException ex)
        {
            return null;
        }
    }
    /**
     * Persists the data. If the model has not been persisted, the userid from
     * creating the model is assigned to userid (if the operation is
     * successful).
     * 
     * Note:
     * - The password minimum length is only checked if non-null.
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
        else if(group == null || group.getGroupid() == -1)
            return PersistStatus_User.InvalidGroup;
        try
        {
            // Validate uniqueness
            if(email != null && (long)conn.executeScalar("SELECT COUNT('') FROM pals_users WHERE email=?;", email) > 0)
                return PersistStatus_User.InvalidEmail_exists;
            else if((long)conn.executeScalar("SELECT COUNT('') FROM pals_users WHERE username=?;", username) > 0)
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
                        group.getGroupid()
                        );
            }
            else
            {
                conn.execute("UPDATE pals_users SET username=?, password=?, password_salt=?, email=?, groupid=? WHERE userid=?",
                        username,
                        password,
                        passwordSalt,
                        email,
                        group.getGroupid(),
                        userid
                        );
            }
            return PersistStatus_User.Success;
        }
        catch(DatabaseException ex)
        {
            core.getLogging().log("[AbstractUser] Failed to persist user.", ex, Logging.EntryType.Warning);
            return PersistStatus_User.Failed;
        }
    }
    // Methods - Mutators ******************************************************
    public void setUsername(String username)
    {
        this.username = username;
    }
    public void setPassword(String password)
    {
        this.password = password;
    }
    public void setPasswordSalt(String passwordSalt)
    {
        this.passwordSalt = passwordSalt;
    }
    public void setEmail(String email)
    {
        this.email = email;
    }
    public void setGroup(UserGroup group)
    {
        this.group = group;
    }
    // Methods - Accessors *****************************************************
    public int getUserid()
    {
        return userid;
    }
    public String getUsername()
    {
        return username;
    }
    public String getPassword()
    {
        return password;
    }
    public String getPasswordSalt()
    {
        return passwordSalt;
    }
    public String getEmail()
    {
        return email;
    }
    public UserGroup getGroup()
    {
        return group;
    }
    // Methods - Accessors - Limits ********************************************
    public int getUsernameMin()
    {
        return 1;
    }
    public int getUsernameMax()
    {
        return 24;
    }
    public int getPasswordMin()
    {
        return 1;
    }
    public int getPasswordMax()
    {
        return 24;
    }
    public int getPasswordSaltMin()
    {
        return 0;
    }
    public int getPasswordSaltMax()
    {
        return 32;
    }
    public int getEmailMin()
    {
        return 1;
    }
    public int getEmailMax()
    {
        return 128;
    }
}
