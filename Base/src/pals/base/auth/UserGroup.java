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

import java.util.ArrayList;
import pals.base.Logging;
import pals.base.NodeCore;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.database.Result;

/**
 * The user-group of a user.
 * 
 * @version 1.0
 */
public class UserGroup
{
    // Enums *******************************************************************
    /**
     * The status from persisting this model.
     * 
     * @since 1.0
     */
    public enum PersistStatus_UserGroup
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
         * The title length is too short or long.
         * 
         * @since 1.0
         */
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
    /**
     * Creates a new unpersisted instance.
     * 
     * @param title The title of the user-group.
     * @param userLogin Users can login.
     * @param markerGeneral Users can mark work.
     * @param adminModules Users can manage modules.
     * @param adminQuestions Users can manage questions.
     * @param adminUsers Users can manage users.
     * @param adminSystem Users can manage system.
     * @since 1.0
     */
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
     * @since 1.0
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
     * @since 1.0
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
     * @since 1.0
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
     * @since 1.0
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
     * @since 1.0
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
     * Sets the title.
     * 
     * @param title Sets the title of the group.
     * @since 1.0
     */
    public void setTitle(String title)
    {
        this.title = title;
    }
    /**
     * Sets login access.
     * 
     * @param userLogin Sets if the group can login.
     * @since 1.0
     */
    public void setUserLogin(boolean userLogin)
    {
        this.userLogin = userLogin;
    }
    /**
     * Sets marker permission.
     * 
     * @param markerGeneral Sets if the group can mark assignments.
     * @since 1.0
     */
    public void setMarkerGeneral(boolean markerGeneral)
    {
        this.markerGeneral = markerGeneral;
    }
    /**
     * Sets modules permission.
     * 
     * @param adminModules Sets if the group can manage modules.
     * @since 1.0
     */
    public void setAdminModules(boolean adminModules)
    {
        this.adminModules = adminModules;
    }
    /**
     * Sets questions permission.
     * 
     * @param adminQuestions Sets if the group can manage questions.
     * @since 1.0
     */
    public void setAdminQuestions(boolean adminQuestions)
    {
        this.adminQuestions = adminQuestions;
    }
    /**
     * Sets users permission.
     * 
     * @param adminUsers Sets if the group can manage users.
     * @since 1.0
     */
    public void setAdminUsers(boolean adminUsers)
    {
        this.adminUsers = adminUsers;
    }
    /**
     * Sets system permission.
     * 
     * @param adminSystem Sets if the group can manage the system.
     * @since 1.0
     */
    public void setAdminSystem(boolean adminSystem)
    {
        this.adminSystem = adminSystem;
    }
    // Methods - Accessors *****************************************************
    /**
     * Indicates if the model has been persisted.
     * 
     * @return True = persisted, false = not persisted.
     * @since 1.0
     */
    public boolean isPersisted()
    {
        return groupid != -1;
    }
    /**
     * Group identifier.
     * 
     * @return The identifier of this group.
     * @since 1.0
     */
    public int getGroupID()
    {
        return groupid;
    }
    /**
     * The title.
     * 
     * @return The title of the group.
     * @since 1.0
     */
    public String getTitle()
    {
        return title;
    }
    /**
     * Login permission.
     * 
     * @return Indicates if the group can login.
     * @since 1.0
     */
    public boolean isUserLogin()
    {
        return userLogin;
    }
    /**
     * Marker permission.
     * 
     * @return Indicates if the group can mark assignments.
     * @since 1.0
     */
    public boolean isMarkerGeneral()
    {
        return markerGeneral;
    }
    /**
     * Admin indicator.
     * 
     * @return Indicates if the group has any admin roles.
     * @since 1.0
     */
    public boolean isAdmin()
    {
        return adminModules || adminQuestions || adminSystem || adminUsers;
    }
    /**
     * Modules permission.
     * 
     * @return Indicates if the group can manage modules.
     * @since 1.0
     */
    public boolean isAdminModules()
    {
        return adminModules;
    }
    /**
     * Questions permission.
     * 
     * @return Indicates if the group can manage questions.
     * @since 1.0
     */
    public boolean isAdminQuestions()
    {
        return adminQuestions;
    }
    /**
     * Users permission.
     * 
     * @return Indicates if the group can manage users.
     * @since 1.0
     */
    public boolean isAdminUsers()
    {
        return adminUsers;
    }
    /**
     * System permission.
     * 
     * @return Indicates if the group can manage the system.
     * @since 1.0
     */
    public boolean isAdminSystem()
    {
        return adminSystem;
    }
    /**
     * Total users in group.
     * 
     * @param conn Database connector.
     * @return The number of users in the group; this is not cached.
     * @since 1.0
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
     * Title minimum length.
     * 
     * @return Minimum length of a title.
     * @since 1.0
     */
    public int getTitleMin()
    {
        return 1;
    }
    /**
     * Title maximum length.
     * 
     * @return Maximum length of a title.
     * @since 1.0
     */
    public int getTitleMax()
    {
        return 64;
    }

    // Methods - Overrides *****************************************************
    /**
     * Tests if an instance is equal to the current instance, based on being
     * the same type and having the same identifier.
     * 
     * @param o The object being tested.
     * @return True = same, false = not the same.
     * @since 1.0
     */
    @Override
    public boolean equals(Object o)
    {
        if(o == null || !(o instanceof UserGroup))
            return false;
        UserGroup a = (UserGroup)o;
        return a.groupid == groupid;
    }
    /**
     * The hash-code, based on the group identifier.
     * 
     * @return The hash-code.
     * @since 1.0
     */
    @Override
    public int hashCode()
    {
        return groupid;
    }
}
