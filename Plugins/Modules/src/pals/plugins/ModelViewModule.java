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
