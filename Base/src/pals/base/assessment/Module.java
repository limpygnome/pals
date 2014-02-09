package pals.base.assessment;

import java.util.ArrayList;
import pals.base.Logging;
import pals.base.NodeCore;
import pals.base.auth.User;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.database.Result;

/**
 * Represents a (course) module; the system allows users/students to be enrolled
 * on different modules, which offer different assignments.
 */
public class Module
{
    // Enums *******************************************************************
    /**
     * The status from persisting the module.
     */
    public enum ModulePersistStatus
    {
        Success,
        Failed,
        Failed_title_length
    }
    // Fields ******************************************************************
    private int     moduleid;   // The module's identifier.
    private String  title;      // The title of the module.
    // Methods - Constructors **************************************************
    /**
     * Creates a new unpersisted model.
     */
    public Module()
    {
        this(null);
    }
    /**
     * Creates a new unpersisted model.
     * 
     * @param title The title of the module.
     */
    public Module(String title)
    {
        this.moduleid = -1;
        this.title = title;
    }
    // Methods - Persistence ***************************************************
    /**
     * @param conn Database connector.
     * @return An array with all of the modules (can be empty).
     */
    public static Module[] loadAll(Connector conn)
    {
        ArrayList<Module> modules = new ArrayList<>();
        try
        {
            // Fetch results from query
            Result q = conn.read("SELECT * FROM pals_modules;");
            // Load each module
            Module m;
            while(q.next())
            {
                if((m = load(q)) != null)
                    modules.add(m);
            }
            return modules.toArray(new Module[modules.size()]);
        }
        catch(DatabaseException ex)
        {
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return new Module[0];
        }
    }
    /**
     * @param conn Database connector.
     * @param user The user enrolled within the modules.
     * @return Array of modules, of which the specified user is enrolled within.
     */
    public static Module[] load(Connector conn, User user)
    {
        try
        {
            ArrayList<Module> modules = new ArrayList<>();
            Result res = conn.read("SELECT * FROM pals_modules WHERE moduleid IN (SELECT moduleid FROM pals_modules_enrollment WHERE userid=?);", user.getUserID());
            Module m;
            while(res.next())
            {
                m = load(res);
                if(m != null)
                    modules.add(m);
            }
            return modules.toArray(new Module[modules.size()]);
        }
        catch(DatabaseException ex)
        {
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return new Module[0];
        }
    }
    /**
     * @param conn Database connector.
     * @param moduleid The identifier of the module.
     * @return An instance of the model or null.
     */
    public static Module load(Connector conn, int moduleid)
    {
        try
        {
            Result res = conn.read("SELECT * FROM pals_modules WHERE moduleid=?;", moduleid);
            return res.next() ? load(res) : null;
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
     * @param result The result instance from a query, with next() already
     * invoked.
     * @return An instance of the model or null.
     */
    public static Module load(Result result)
    {
        try
        {
            Module m = new Module((String)result.get("title"));
            m.moduleid = result.get("moduleid");
            return m;
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
     * Persists the model.
     * 
     * @param conn Database connector.
     * @return Status of persisting the model.
     */
    public ModulePersistStatus persist(Connector conn)
    {
        // Validate data
        if(title.length() < getTitleMin() || title.length() > getTitleMax())
            return ModulePersistStatus.Failed_title_length;
        // Persist data
        try
        {
            if(moduleid == -1)
            {
                moduleid = (int)conn.executeScalar("INSERT INTO pals_modules (title) VALUES(?) RETURNING moduleid;", title);
            }
            else
            {
                conn.execute("UPDATE pals_modules SET title=? WHERE moduleid=?;", title, moduleid);
            }
            return ModulePersistStatus.Success;
        }
        catch(DatabaseException ex)
        {
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return ModulePersistStatus.Failed;
        }
    }
    /**
     * Unpersists the model from the database; can be re-used.
     * 
     * @param conn Database connector.
     */
    public void delete(Connector conn)
    {
        try
        {
            conn.execute("DELETE FROM pals_modules WHERE moduleid=?;", moduleid);
            this.moduleid = -1;
        }
        catch(DatabaseException ex)
        {
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
        }
    }
    // Methods *****************************************************************
    /**
     * Sets the users of a module; previous users are removed. This action is
     * not done within a transaction.
     * 
     * @param conn Database connector.
     * @param users The users of the module.
     * @return True = added, false = failed/error.
     */
    public boolean usersSet(Connector conn, User[] users)
    {
        if(!usersRemoveAll(conn))
            return false;
        else
            return usersAdd(conn, users);
    }
    /**
     * Adds a user on the module.
     * 
     * @param conn Database connector.
     * @param user The user to add to the module.
     * @return True = added, false = failed/error.
     */
    public boolean usersAdd(Connector conn, User user)
    {
        if(moduleid == -1)
            return false;
        try
        {
            conn.execute("INSERT INTO pals_modules_enrollment(moduleid,userid) VALUES(?,?);", moduleid, user.getUserID());
            return true;
        }
        catch(DatabaseException ex)
        {
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return false;
        }
    }
    /**
     * Adds multiple users to the module.
     * 
     * @param conn Database connector.
     * @param users The users to add; the user should not be already added to
     * the module or this will fail.
     * @return True = added, false = failed/error.
     */
    public boolean usersAdd(Connector conn, User[] users)
    {
        if(moduleid == -1)
            return false;
        else if(users.length == 0)
            return true;
        try
        {
            StringBuilder q = new StringBuilder("INSERT INTO pals_modules_enrollment (moduleid, userid) VALUES");
            for(User user : users)
            {
                q.append("(").append(moduleid).append(",").append((int)user.getUserID()).append("),"); // casted to int in-case of type change in the future
            }
            q.deleteCharAt(q.length()-1).append(";");
            conn.execute(q.toString());
            return true;
        }
        catch(DatabaseException ex)
        {
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return false;
        }
    }
    /**
     * Removes a user from the module.
     * 
     * @param conn Database connector.
     * @param user Removes a user
     * @return True = added, false = failed/error.
     */
    public boolean usersRemove(Connector conn, User user)
    {
        try
        {
            conn.execute("DELETE FROM pals_modules_enrollment WHERE moduleid=? AND userid=?;", moduleid, user.getUserID());
            return true;
        }
        catch(DatabaseException ex)
        {
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return false;
        }
    }
    /**
     * Removes multiple users from a module.
     * 
     * @param conn Database connector.
     * @param users The users to remove.
     * @return True = added, false = failed/error.
     */
    public boolean usersRemove(Connector conn, User[] users)
    {
        // Build query - casting to protect against possible future data-type changes
        StringBuilder q = new StringBuilder("DELETE FROM pals_modules_enrollment WHERE moduleid=");
        q.append((int)moduleid).append(" AND (");
        for(User user : users)
        {
            q.append("userid=").append((int)user.getUserID()).append(" OR ");
        }
        q.replace(q.length()-5, q.length(), "").append(");");
        // Execute query
        try
        {
            conn.execute(q.toString());
            return true;
        }
        catch(DatabaseException ex)
        {
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return false;
        }
    }
    /**
     * Removes all of the users on a module.
     * 
     * @param conn Database connector.
     * @return True = added, false = failed/error.
     */
    public boolean usersRemoveAll(Connector conn)
    {
        try
        {
            conn.execute("DELETE FROM pals_modules_enrollment WHERE moduleid=?;", moduleid);
            return true;
        }
        catch(DatabaseException ex)
        {
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return false;
        }
    }
    /**
     * Fetches all of the users enrolled on a module; this is an expensive
     * operation.
     * 
     * @param conn Database connector.
     * @return Array of users; may be empty.
     */
    public User[] usersEnrolled(Connector conn)
    {
        try
        {
            ArrayList<User> users = new ArrayList<>();
            // Execute query and parse results
            Result q = conn.read("SELECT * FROM pals_users WHERE userid IN (SELECT userid FROM pals_modules_enrollment WHERE moduleid=?);", moduleid);
            User u;
            while(q.next())
            {
                if((u = User.load(conn, q)) != null)
                    users.add(u);
            }
            return users.toArray(new User[users.size()]);
        }
        catch(DatabaseException ex)
        {
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return new User[0];
        }
    }
    // Methods - Mutators ******************************************************
    /**
     * @param title The new title for the module.
     */
    public void setTitle(String title)
    {
        this.title = title;
    }
    // Methods - Accessors *****************************************************
    /**
     * @return Indicates if the model has been persisted.
     */
    public boolean isPersisted()
    {
        return moduleid != -1;
    }
    /**
     * @return The identifier of the module.
     */
    public int getModuleID()
    {
        return moduleid;
    }
    /**
     * @return The title of the module.
     */
    public String getTitle()
    {
        return title;
    }
    /**
     * @param conn Database connector.
     * @return Total number of enrolled users.
     */
    public int getUsersEnrolled(Connector conn)
    {
        try
        {
            return (int)(long)conn.executeScalar("SELECT COUNT('') FROM pals_modules_enrollment WHERE moduleid=?;", moduleid);
        }
        catch(DatabaseException ex)
        {
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return 0;
        }
    }
    /**
     * @param conn Database connector.
     * @return Total number of assignments for this module.
     */
    public int getTotalAssignments(Connector conn)
    {
        try
        {
            return (int)(long)conn.executeScalar("SELECT COUNT('') FROM pals_assignment WHERE moduleid=?;", moduleid);
        }
        catch(DatabaseException ex)
        {
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return 0;
        }
    }
    /**
     * @param conn Database connector.
     * @param user User to check.
     * @return Indicates if the user is enrolled on the module.
     */
    public boolean isEnrolled(Connector conn, User user)
    {
        try
        {
            return (long)conn.executeScalar("SELECT COUNT('') FROM pals_modules_enrollment WHERE moduleid=? AND userid=?;", moduleid, user.getUserID()) == 1;
        }
        catch(DatabaseException ex)
        {
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return false;
        }
    }
    // Methods - Accessors - Limits ********************************************
    /**
     * @return The minimum length of a module title.
     */
    public int getTitleMin()
    {
        return 1;
    }
    /**
     * @return Maximum length of a module title.
     */
    public int getTitleMax()
    {
        return 64;
    }
}
