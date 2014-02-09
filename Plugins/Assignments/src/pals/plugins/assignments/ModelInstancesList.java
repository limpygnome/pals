package pals.plugins.assignments;

import java.util.ArrayList;
import org.joda.time.DateTime;
import pals.base.assessment.Assignment;
import pals.base.assessment.InstanceAssignment;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.database.Result;

/**
 * A model with information about instances of assignments, for marking.
 */
public class ModelInstancesList
{
    // Fields ******************************************************************
    private int         iaAIID;
    private String      iaUsername;
    private DateTime    iaTimeEnd;
    // Methods - Constructors **************************************************
    public ModelInstancesList(int iaAIID, String iaUsername, DateTime iaTimeEnd)
    {
        this.iaAIID = iaAIID;
        this.iaUsername = iaUsername;
        this.iaTimeEnd = iaTimeEnd;
    }
    // Methods - Persistence ***************************************************
    /**
     * @param conn Database connector.
     * @return Array of instance-assignment information for an assignment.
     */
    public static ModelInstancesList[] load(Connector conn, Assignment ass)
    {
        try
        {
            Result res = conn.read("SELECT ai.aiid, ai.time_end, u.username FROM pals_assignment_instance AS ai LEFT OUTER JOIN pals_users AS u ON u.userid=ai.userid WHERE ai.assid=? AND ai.status=? ORDER BY ai.time_end ASC;", ass.getAssID(), InstanceAssignment.Status.Submitted.getStatus());
            ArrayList<ModelInstancesList> buffer = new ArrayList<>();
            ModelInstancesList t;
            while(res.next())
            {
                if((t = load(res)) != null)
                    buffer.add(t);
            }
            return buffer.toArray(new ModelInstancesList[buffer.size()]);
        }
        catch(DatabaseException ex)
        {
            return new ModelInstancesList[0];
        }
    }
    private static ModelInstancesList load(Result res)
    {
        try
        {
            Object t = res.get("time_end");
            return new ModelInstancesList((int)res.get("aiid"), (String)res.get("username"), t != null ? new DateTime(t) : null);
        }
        catch(DatabaseException ex)
        {
            return null;
        }
    }

    // Methods - Accessors *****************************************************
    /**
     * @return Instance-assignment identifier.
     */
    public int getIaAIID()
    {
        return iaAIID;
    }
    /**
     * @return Instance-assignment user's username.
     */
    public String getIaUsername()
    {
        return iaUsername;
    }
    /**
     * @return The time at which the assignment was submitted; can be null.
     */
    public DateTime getIaTimeEnd()
    {
        return iaTimeEnd;
    }
}
