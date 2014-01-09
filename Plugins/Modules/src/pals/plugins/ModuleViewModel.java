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
public class ModuleViewModel
{
    // Fields ******************************************************************
    private Assignment  ass;
    private double      markHighest;
    private double      markLast;
    private int         attempts;
    // Methods - Constructors **************************************************
    public ModuleViewModel(Connector conn, Assignment ass, User user)
    {
        this.ass = ass;
        try
        {
            Result res = conn.read("SELECT "
                    +"(SELECT COUNT('') FROM pals_assignment_instance WHERE userid=? AND assid=?) AS attempts,"
                    +"COALESCE((SELECT mark FROM pals_assignment_instance WHERE userid=? AND assid=? AND status=? ORDER BY mark DESC LIMIT 1),-1) AS highest,"
                    +"COALESCE((SELECT mark FROM pals_assignment_instance WHERE userid=? AND assid=? AND status=? ORDER BY aiid DESC LIMIT 1),-1) AS last"
                    +";",
                    user.getUserID(), ass.getAssID(),
                    user.getUserID(), ass.getAssID(), InstanceAssignment.Status.Marked.getStatus(),
                    user.getUserID(), ass.getAssID(), InstanceAssignment.Status.Marked.getStatus()
            );
            res.next();
            this.markHighest = (double)res.get("highest");
            this.markLast = (double)res.get("last");
            this.attempts = (int)(long)res.get("attempts");
        }
        catch(DatabaseException ex)
        {
            this.markHighest = this.markLast = -1;
            this.attempts = -1;
        }
    }
    // Methods - Accessors *****************************************************
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
