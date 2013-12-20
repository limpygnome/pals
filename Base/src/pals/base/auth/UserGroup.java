package pals.base.auth;

import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.database.Result;

/**
 * The user-group of a user.
 */
public class UserGroup
{
    // Enums *******************************************************************
    /**
     * The status from persisting this model.
     */
    public enum PersistStatus_UserGroup
    {
        Success,
        Failed,
        Title_Length
    }
    // Fields ******************************************************************
    private int     groupid;            // Group identifier.
    private String  title;              // The title of the group.
    private boolean userLogin,          // Indicates if the user can login.
                    markerGeneral,      // Indictaes the user is allowed to mark work.
                    adminModules,       // Indicates the user is allowed to manage modules.
                    adminAssignments,   // Indicates the user is allowed to manage assignments.
                    adminUsers,         // Indicates the user is allowed to manage users.
                    adminSystem;        // Indicates the user is allowed to manage the system.
    // Methods - Constructors **************************************************
    private UserGroup(int groupid, String title, boolean userLogin, boolean markerGeneral, boolean adminModules, boolean adminAssignments, boolean adminUsers, boolean adminSystem)
    {
        this.groupid = groupid;
        this.title = title;
        this.userLogin = userLogin;
        this.markerGeneral = markerGeneral;
        this.adminModules = adminModules;
        this.adminAssignments = adminAssignments;
        this.adminUsers = adminUsers;
        this.adminSystem = adminSystem;
    }
    // Methods - Persistence ***************************************************
    /**
     * @return A new, unpersisted, user-group.
     */
    public static UserGroup create()
    {
        return new UserGroup(-1, "Untitled Group", false, false, false, false, false, false);
    }
    /**
     * Loads a user-group by its identifier.
     * 
     * @param conn Database connector.
     * @param groupid Group identifier.
     * @return Instance of group or null.
     */
    public static UserGroup load(Connector conn, int groupid)
    {
        try
        {
            Result res = conn.read("SELECT * FROM pals_users_group WHERE groupid=?;", groupid);
            return res.next() ? load(res) : null;
        }
        catch(DatabaseException ex)
        {
            return null;
        }
    }
    /**
     * Loads a user-group from query result.
     * 
     * @param result Query result; next() should be pre-invoked.
     * @return Instance of group or null.
     */
    public static UserGroup load(Result result)
    {
        try
        {
            return new UserGroup(
                    (int)result.get("groupid"),
                    (String)result.get("title"),
                    ((String)result.get("user_login")).equals("1"),
                    ((String)result.get("marker_general")).equals("1"),
                    ((String)result.get("admin_modules")).equals("1"),
                    ((String)result.get("admin_assignments")).equals("1"),
                    ((String)result.get("admin_users")).equals("1"),
                    ((String)result.get("admin_system")).equals("1")
            );
        }
        catch(DatabaseException ex)
        {
            return null;
        }
    }
    /**
     * Persists the data; a new user-group will be assigned a groupid if
     * the operation is successful.
     * 
     * @param conn Database connector.
     * @return Status of the operation.
     */
    public PersistStatus_UserGroup persist(Connector conn)
    {
        // Validate data
        if(title == null || title.length() < getTitleMin() || title.length() > getTitleMax())
            return PersistStatus_UserGroup.Title_Length;
        // Persist the data
        try
        {
            if(groupid == -1)
            {
                groupid = (int)conn.executeScalar("INSERT INTO pals_users_group (title, user_login, marker_general, admin_modules, admin_assignments, admin_users, admin_system) VALUES(?,?,?,?,?,?,?) "
                        + "RETURNING groupid;"
                        ,
                        title,
                        userLogin,
                        markerGeneral,
                        adminModules,
                        adminAssignments,
                        adminUsers,
                        adminSystem
                        );
            }
            else
            {
                conn.execute("UPDATE pals_users_group SET title=?, user_login=?, marker_general=?, admin_modules=?, admin_assignments=?, admin_users=?, admin_system=? WHERE groupid=?;",
                        title,
                        userLogin ? "1" : "0",
                        markerGeneral ? "1" : "0",
                        adminModules ? "1" : "0",
                        adminAssignments ? "1" : "0",
                        adminUsers ? "1" : "0",
                        adminSystem ? "1" : "0",
                        groupid
                        );
            }
            return PersistStatus_UserGroup.Success;
        }
        catch(DatabaseException ex)
        {
            return PersistStatus_UserGroup.Failed;
        }
    }
    // Methods - Mutators ******************************************************
    /**
     * @param title Sets the title of the group.
     */
    public void setTitle(String title)
    {
        this.title = title;
    }
    /**
     * @param userLogin Sets if the group can login.
     */
    public void setUserLogin(boolean userLogin)
    {
        this.userLogin = userLogin;
    }
    /**
     * @param markerGeneral Sets if the group can mark assignments.
     */
    public void setMarkerGeneral(boolean markerGeneral)
    {
        this.markerGeneral = markerGeneral;
    }
    /**
     * @param adminModules Sets if the group can manage modules.
     */
    public void setAdminModules(boolean adminModules)
    {
        this.adminModules = adminModules;
    }
    /**
     * @param adminAssignments Sets if the group can manage assignments.
     */
    public void setAdminAssignments(boolean adminAssignments)
    {
        this.adminAssignments = adminAssignments;
    }
    /**
     * @param adminUsers Sets if the group can manage users.
     */
    public void setAdminUsers(boolean adminUsers)
    {
        this.adminUsers = adminUsers;
    }
    /**
     * @param adminSystem Sets if the group can manage the system.
     */
    public void setAdminSystem(boolean adminSystem)
    {
        this.adminSystem = adminSystem;
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The identifier of this group.
     */
    public int getGroupid()
    {
        return groupid;
    }
    /**
     * @return The title of the group.
     */
    public String getTitle()
    {
        return title;
    }
    /**
     * @return Indicates if the group can login.
     */
    public boolean isUserLogin()
    {
        return userLogin;
    }
    /**
     * @return Indicates if the group can mark assignments.
     */
    public boolean isMarkerGeneral()
    {
        return markerGeneral;
    }
    /**
     * @return Indicates if the group can manage modules.
     */
    public boolean isAdminModules()
    {
        return adminModules;
    }
    /**
     * @return Indicates if the group can manage assignments.
     */
    public boolean isAdminAssignments()
    {
        return adminAssignments;
    }
    /**
     * @return Indicates if the group can manage users.
     */
    public boolean isAdminUsers()
    {
        return adminUsers;
    }
    /**
     * @return Indicates if the group can manage the system.
     */
    public boolean isAdminSystem()
    {
        return adminSystem;
    }
    // Methods - Accessors - Limits ********************************************
    /**
     * @return Minimum length of a title.
     */
    public int getTitleMin()
    {
        return 1;
    }
    /**
     * @return Maximum length of a title.
     */
    public int getTitleMax()
    {
        return 64;
    }
}
