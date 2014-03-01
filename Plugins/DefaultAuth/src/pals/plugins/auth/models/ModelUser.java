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
    private String  username,
                    email;
    // Methods - Constructors **************************************************
    public ModelUser(int userid, String username, String email)
    {
        this.userid = userid;
        this.username = username;
        this.email = email;
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
                res = conn.read("SELECT userid, username, email FROM pals_users WHERE username LIKE ? OR email LIKE ? ORDER BY username ASC LIMIT ? OFFSET ?;", "%"+filter.replace("%", "")+"%", "%"+filter.replace("%", "")+"%", amount, offset);
            else
                res = conn.read("SELECT userid, username, email FROM pals_users ORDER BY username ASC LIMIT ? OFFSET ?;", amount, offset);
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
            return new ModelUser((int)res.get("userid"), (String)res.get("username"), (String)res.get("email"));
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
    /**
     * @return The user's e-mail.
     */
    public String getEmail()
    {
        return email;
    }
}
