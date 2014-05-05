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
package pals.plugins;

import java.util.ArrayList;
import java.util.HashMap;
import pals.base.assessment.Assignment;
import pals.base.assessment.Module;
import pals.base.auth.User;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.database.Result;

/**
 * Fetches information for the highest marks for instances of an assignment,
 * for each user; if the user has no instance, their mark is zero and a null
 * integer is set.
 * 
 * @version 1.0
 */
public class ModelAssHighest
{
    // Classes *****************************************************************
    /**
     * Represents the marks and overall mark for a module.
     * 
     * @version 1.0
     */
    public static class ModelModule
    {
        // Fields ***************************************************************
        private ModelAssHighest[]   marks;
        private double              mark;
        // Methods - Constructors ***********************************************
        private ModelModule(ModelAssHighest[] marks)
        {
            this.marks = marks;
            // Calculate overall mark
            double sumMark = 0, sumWeight = 0;
            for(ModelAssHighest mah : marks)
            {
                sumMark += mah.mark * mah.weight;
                sumWeight += mah.weight;
            }
            mark = sumMark / sumWeight;
        }
        // Methods - Accessors **************************************************
        /**
         * The highest marks of the module's assignments.
         * 
         * @return An array of highest marks; can be empty.
         * @since 1.0
         */
        public ModelAssHighest[] getMarks()
        {
            return marks;
        }
        /**
         * The overall mark for the module.
         * 
         * @return The mark from 0 to 100.
         * @since 1.0
         */
        public double getMark()
        {
            return mark;
        }
        /**
         * Retrieves the user identifier.
         * 
         * @return The identifier of the user.
         * @since 1.0
         */
        public int getUserID()
        {
            return marks[0].getUserID();
        }
        /**
         * Retrieves the username of the user.
         * 
         * @return The username.
         * @since 1.0
         */
        public String getUsername()
        {
            return marks[0].getUsername();
        }
    }
    // Fields ******************************************************************
    private Integer     aiid;
    private int         userid;
    private String      username;
    private double      mark,
                        weight;
    private Assignment ass;
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
     * Loads all of the highest mark for each assignment, for a module.
     * 
     * @param conn Database connector.
     * @param m The module.
     * @param u The user filter; can be optional to load all users.
     * @return User identifiers mapped to array of assignment marks.
     * @since 1.0
     */
    public static ModelModule[] loadModule(Connector conn, Module m, User u)
    {
        ArrayList<ModelModule> buffer = new ArrayList<>();
        try
        {
            // Fetch all the assignments for the model
            HashMap<Integer, Assignment> mapAss = new HashMap<>();
            // Create hash-map of assignment models to IDs
            {
                Assignment[] ab = Assignment.load(conn, m, false);
                for(Assignment a : ab)
                    mapAss.put(a.getAssID(), a);
            }
            // Load marks
            Result res = conn.read("SELECT a.assid, a.weight, ai.aiid, me.userid, u.username, COALESCE(ai.mark, 0) AS mark FROM "+
                    "pals_modules_enrollment AS me JOIN pals_assignment AS a ON a.moduleid=me.moduleid "+
                    "LEFT OUTER JOIN pals_assignment_instance AS ai ON (ai.aiid=(SELECT aiid FROM pals_assignment_instance "+
                    "WHERE userid=me.userid AND assid=a.assid ORDER BY mark DESC LIMIT 1)) LEFT OUTER JOIN pals_users AS u ON u.userid=me.userid "+
                    "WHERE me.moduleid=?"+(u != null ? " AND me.userid=?" : "")+" ORDER BY u.username ASC, a.assid ASC;",
                    u != null ? new Object[]{m.getModuleID(),u.getUserID()} : new Object[]{m.getModuleID()});
            
            ArrayList<ModelAssHighest> arrBuffer = new ArrayList<>();
            int currUserID = -1;
            ModelAssHighest mah;
            ModelModule mm;
            while(res.next())
            {
                // Parse model
                if((mah = load(res)) != null)
                {
                    // Check if the user has changed
                    if(mah.userid != currUserID)
                    {
                        // Move previous buffer to map
                        if(!arrBuffer.isEmpty())
                        {
                            buffer.add(new ModelModule(arrBuffer.toArray(new ModelAssHighest[arrBuffer.size()])));
                            arrBuffer.clear();
                        }
                        // Switch current identifier
                        currUserID = mah.userid;
                    }
                    // Set assignment ref for model
                    mah.ass = mapAss.get((int)res.get("assid"));
                    // Set custom fields outside the model
                    mah.weight = (int)res.get("weight");
                    // Add model to buffer
                    arrBuffer.add(mah);
                }
            }
            // Move final result to buffer
            if(!arrBuffer.isEmpty())
            {
                buffer.add(new ModelModule(arrBuffer.toArray(new ModelAssHighest[arrBuffer.size()])));
                arrBuffer.clear();
            }
        }
        catch(DatabaseException ex)
        {
        }
        return buffer.toArray(new ModelModule[buffer.size()]);
    }
    
    /**
     * Loads the highest marks for a module.
     * 
     * @param conn Database connector.
     * @param ass Assignment being viewed.
     * @return Array of highest marks; can be empty.
     * @since 1.0
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
     * The identifier of the instance of the assignment.
     * 
     * @return Can be null if no instance was taken by the user.
     * @since 1.0
     */
    public Integer getAIID()
    {
        return aiid;
    }
    /**
     * Retrieves the user's identifier.
     * 
     * @return The user's identifier.
     * @since 1.0
     */
    public int getUserID()
    {
        return userid;
    }
    /**
     * Retrieves the user's username.
     * 
     * @return The user's username.
     * @since 1.0
     */
    public String getUsername()
    {
        return username;
    }
    /**
     * Retrieves the user's highest mark for the assignment.
     * 
     * @return Either 0, if no instance exists, or the mark of the instance
     * with the highest mark.
     * @since 1.0
     */
    public double getMark()
    {
        return mark;
    }
    /**
     * Retrieves the assignment model associated with this model. This is only
     * loaded when loading the models for a module, using
     * {@link #loadModule(pals.base.database.Connector, pals.base.assessment.Module)}.
     * 
     * @return An instance or null.
     * @since 1.0
     */
    public Assignment getAss()
    {
        return ass;
    }
}
