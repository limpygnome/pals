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
package pals.plugins;

import java.util.ArrayList;
import pals.base.assessment.Assignment;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.database.Result;

/**
 * Fetches information for the highest marks for instances of an assignment,
 * for each user; if the user has no instance, their mark is zero and a null
 * integer is set.
 */
public class ModelAssHighest
{
    // Fields ******************************************************************
    private Integer aiid;
    private int     userid;
    private String  username;
    private double  mark;
    // Methods - Constructors **************************************************
    private ModelAssHighest(Integer aiid, int userid, String username, double mark)
    {
        this.aiid = aiid;
        this.userid = userid;
        this.username = username;
        this.mark = mark;
    }
    // Methods - Persistence ***************************************************
    /**
     * @param conn Database connector.
     * @param ass Assignment being viewed.
     * @return Array of highest marks; can be empty.
     */
    public static ModelAssHighest[] load(Connector conn, Assignment ass)
    {
        try
        {
            Result res = conn.read("SELECT ai.aiid, me.userid, u.username, COALESCE(ai.mark, 0) AS mark FROM pals_modules_enrollment AS me LEFT OUTER JOIN pals_assignment_instance AS ai ON (ai.aiid=(SELECT aiid FROM pals_assignment_instance WHERE userid=me.userid AND assid=? ORDER BY mark DESC LIMIT 1)) LEFT OUTER JOIN pals_users AS u ON u.userid=me.userid WHERE me.moduleid=? ORDER BY mark DESC, u.username ASC;", ass.getAssID(), ass.getModule().getModuleID());
            ArrayList<ModelAssHighest> buffer = new ArrayList<>();
            ModelAssHighest m;
            while(res.next())
            {
                if((m = load(res)) != null)
                    buffer.add(m);
            }
            return buffer.toArray(new ModelAssHighest[buffer.size()]);
        }
        catch(DatabaseException ex)
        {
            return new ModelAssHighest[0];
        }
    }
    private static ModelAssHighest load(Result res)
    {
        try
        {
            return new ModelAssHighest((Integer)res.get("aiid"), (int)res.get("userid"), (String)res.get("username"), (double)res.get("mark"));
        }
        catch(DatabaseException ex)
        {
            return null;
        }
    }

    // Methods - Accessors *****************************************************
    /**
     * @return The identifier of the instance of the assignment; can be null if
     * no instance was taken by the user.
     */
    public Integer getAIID()
    {
        return aiid;
    }
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
     * @return The user's highest mark for the assignment; this is 0 if no
     * instance exists.
     */
    public double getMark()
    {
        return mark;
    }
}
