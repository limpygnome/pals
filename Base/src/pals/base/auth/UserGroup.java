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

import java.util.ArrayList;
import pals.base.Logging;
import pals.base.NodeCore;
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
                    adminQuestions,     // Indicates the user is allowed to manage questions.
                    adminUsers,         // Indicates the user is allowed to manage users.
                    adminSystem;        // Indicates the user is allowed to manage the system.
    // Methods - Constructors **************************************************
    public UserGroup(String title, boolean userLogin, boolean markerGeneral, boolean adminModules, boolean adminQuestions, boolean adminUsers, boolean adminSystem)
    {
        this.groupid = -1;
        this.title = title;
        this.userLogin = userLogin;
        this.markerGeneral = markerGeneral;
        this.adminModules = adminModules;
        this.adminQuestions = adminQuestions;
        this.adminUsers = adminUsers;
        this.adminSystem = adminSystem;
    }
    // Methods - Persistence ***************************************************
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
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return null;
        }
    }
    /**
     * Loads all of the user-groups on the system.
     * 
     * @param conn Database connector.
     * @return Array of user-groups; can be empty.
     */
    public static UserGroup[] load(Connector conn)
    {
        try
        {
            Result res = conn.read("SELECT * FROM pals_users_group ORDER BY title ASC;");
            ArrayList<UserGroup> buffer = new ArrayList<>();
            UserGroup t;
            while(res.next())
            {
                if((t = load(res)) != null)
                    buffer.add(t);
            }
            return buffer.toArray(new UserGroup[buffer.size()]);
        }
        catch(DatabaseException ex)
        {
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return new UserGroup[0];
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
            UserGroup ug = new UserGroup(
                    (String)result.get("title"),
                    ((String)result.get("user_login")).equals("1"),
                    ((String)result.get("marker_general")).equals("1"),
                    ((String)result.get("admin_modules")).equals("1"),
                    ((String)result.get("admin_questions")).equals("1"),
                    ((String)result.get("admin_users")).equals("1"),
                    ((String)result.get("admin_system")).equals("1")
            );
            ug.groupid = (int)result.get("groupid");
            return ug;
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
                groupid = (int)conn.executeScalar("INSERT INTO pals_users_group (title, user_login, marker_general, admin_modules, admin_questions, admin_users, admin_system) VALUES(?,?,?,?,?,?,?) "
                        + "RETURNING groupid;"
                        ,
                        title,
                        userLogin ? "1" : "0",
                        markerGeneral ? "1" : "0",
                        adminModules ? "1" : "0",
                        adminQuestions ? "1": "0",
                        adminUsers ? "1" : "0",
                        adminSystem ? "1" : "0"
                        );
            }
            else
            {
                conn.execute("UPDATE pals_users_group SET title=?, user_login=?, marker_general=?, admin_modules=?, admin_questions=?, admin_users=?, admin_system=? WHERE groupid=?;",
                        title,
                        userLogin ? "1" : "0",
                        markerGeneral ? "1" : "0",
                        adminModules ? "1" : "0",
                        adminQuestions ? "1" : "0",
                        adminUsers ? "1" : "0",
                        adminSystem ? "1" : "0",
                        groupid
                        );
            }
            return PersistStatus_UserGroup.Success;
        }
        catch(DatabaseException ex)
        {
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return PersistStatus_UserGroup.Failed;
        }
    }
    /**
     * Unpersists the user-group.
     * 
     * @param conn Database connector.
     * @return Indicates if the operation succeeded.
     */
    public boolean remove(Connector conn)
    {
        try
        {
            if(groupid != -1)
            {
                conn.execute("DELETE FROM pals_users_group WHERE groupid=?;", groupid);
                groupid = -1;
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
     * @param adminAssignments Sets if the group can manage questions.
     */
    public void setAdminQuestions(boolean adminAssignments)
    {
        this.adminQuestions = adminAssignments;
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
     * @return Indicates if the model has been persisted.
     */
    public boolean isPersisted()
    {
        return groupid != -1;
    }
    /**
     * @return The identifier of this group.
     */
    public int getGroupID()
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
     * @return Indicates if the group has any admin roles.
     */
    public boolean isAdmin()
    {
        return adminModules || adminQuestions || adminSystem || adminUsers;
    }
    /**
     * @return Indicates if the group can manage modules.
     */
    public boolean isAdminModules()
    {
        return adminModules;
    }
    /**
     * @return Indicates if the group can manage questions.
     */
    public boolean isAdminQuestions()
    {
        return adminQuestions;
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
    /**
     * @param conn Database connector.
     * @return The number of users in the group; this is not cached.
     */
    public int getUserCount(Connector conn)
    {
        if(groupid != -1)
        {
            try
            {
                return (int)(long)conn.executeScalar("SELECT COUNT('') FROM pals_users WHERE groupid=?;", groupid);
            }
            catch(DatabaseException ex)
            {
            }
        }
        return -1;
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
