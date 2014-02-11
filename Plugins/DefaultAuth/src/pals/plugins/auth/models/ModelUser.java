package pals.plugins.auth.models;

import java.util.ArrayList;
import pals.base.auth.UserGroup;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.database.Result;

/**
 * Represents a stripped-down user, with extensions on the base methods to
 * retrieve data.
 */
public class ModelUser
{
    // Fields ******************************************************************
    private int     userid;
    private String  username;
    // Methods - Constructors **************************************************
    public ModelUser(int userid, String username)
    {
        this.userid = userid;
        this.username = username;
    }
    // Methods - Persistence ***************************************************
    /**
     * @param conn Database connector.
     * @param filter Username filter.
     * @param amount The maximum number of users to retrieve.
     * @param offset The offset, amount, of users to retrieve.
     * @return Array of users; can be empty.
     */
    public static ModelUser[] load(Connector conn, String filter, int amount, int offset)
    {
        try
        {
            Result res;
            if(filter != null && filter.length() > 0)
                res = conn.read("SELECT userid, username FROM pals_users WHERE username ORDER BY username ASC LIKE ? LIMIT ? OFFSET ?;", "%"+filter.replace("%", "")+"%", amount, offset);
            else
                res = conn.read("SELECT userid, username FROM pals_users ORDER BY username ASC LIMIT ? OFFSET ?;", amount, offset);
            ArrayList<ModelUser> buffer = new ArrayList<>();
            ModelUser t;
            while(res.next())
            {
                if((t = load(res)) != null)
                    buffer.add(t);
            }
            return buffer.toArray(new ModelUser[buffer.size()]);
        }
        catch(DatabaseException ex)
        {
            return new ModelUser[0];
        }
    }
    /**
     * @param ug The group of users to retrieve.
     * @param conn Database connector.
     * @param amount The maximum number of users to retrieve.
     * @param offset The offset, amount, of users to retrieve.
     * @return Array of users belonging to the specified group; can be empty.
     */
    public static ModelUser[] loadGroup(UserGroup ug, Connector conn, int amount, int offset)
    {
        try
        {
            Result res = conn.read("SELECT userid, username FROM pals_users WHERE groupid=? LIMIT ? OFFSET ?;", ug.getGroupID(), amount, offset);
            ArrayList<ModelUser> buffer = new ArrayList<>();
            ModelUser t;
            while(res.next())
            {
                if((t = load(res)) != null)
                    buffer.add(t);
            }
            return buffer.toArray(new ModelUser[buffer.size()]);
        }
        catch(DatabaseException ex)
        {
            return new ModelUser[0];
        }
    }
    private static ModelUser load(Result res)
    {
        try
        {
            return new ModelUser((int)res.get("userid"), (String)res.get("username"));
        }
        catch(DatabaseException ex)
        {
            return null;
        }
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The user's identifier.
     */
    public int getUserID()
    {
        return userid;
    }
    /**
     * @return The user's username.
     */
    public String getUsername()
    {
        return username;
    }
}
