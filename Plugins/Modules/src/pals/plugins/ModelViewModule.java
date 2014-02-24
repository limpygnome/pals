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

import pals.base.assessment.Assignment;
import pals.base.assessment.InstanceAssignment;
import pals.base.auth.User;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.database.Result;

/**
 * A model for displaying module-assignment information.
 */
public class ModelViewModule
{
    // Fields ******************************************************************
    private Assignment  ass;
    private double      markHighest;
    private double      markLast;
    private int         attempts;
    private boolean     lastActive;
    // Methods - Constructors **************************************************
    public ModelViewModule(Connector conn, Assignment ass, User user)
    {
        this.ass = ass;
        try
        {
            Result res = conn.read("SELECT "
                    +"(SELECT COUNT('') FROM pals_assignment_instance WHERE userid=? AND assid=?) AS attempts,"
                    +"COALESCE((SELECT mark FROM pals_assignment_instance WHERE userid=? AND assid=? AND status=? ORDER BY mark DESC LIMIT 1),-1) AS highest,"
                    +"COALESCE((SELECT mark FROM pals_assignment_instance WHERE userid=? AND assid=? AND status=? ORDER BY aiid DESC LIMIT 1),-1) AS last,"
                    +"(SELECT status FROM pals_assignment_instance WHERE userid=? AND assid=? ORDER BY aiid DESC LIMIT 1) AS last_status"
                    +";",
                    user.getUserID(), ass.getAssID(),
                    user.getUserID(), ass.getAssID(), InstanceAssignment.Status.Marked.getStatus(),
                    user.getUserID(), ass.getAssID(), InstanceAssignment.Status.Marked.getStatus(),
                    user.getUserID(), ass.getAssID()
            );
            res.next();
            this.markHighest = (double)res.get("highest");
            this.markLast = (double)res.get("last");
            this.attempts = (int)(long)res.get("attempts");
            Object t = res.get("last_status");
            this.lastActive = t == null ? false : InstanceAssignment.Status.parse((int)t) == InstanceAssignment.Status.Active;
        }
        catch(DatabaseException ex)
        {
            this.markHighest = this.markLast = -1;
            this.attempts = -1;
        }
    }
    // Methods - Accessors *****************************************************
    public boolean canTake()
    {
        return ass.isActive() && !ass.isDueSurpassed() && (lastActive || ass.getMaxAttempts() == -1 || attempts < ass.getMaxAttempts());
    }
    public Assignment getAss()
    {
        return ass;
    }
    public double getMarkHighest()
    {
        return markHighest;
    }
    public double getMarkLast()
    {
        return markLast;
    }
    public int getAttempts()
    {
        return attempts;
    }
}
