package pals.plugins.assignments;

import java.util.ArrayList;
import org.joda.time.DateTime;
import pals.base.assessment.InstanceAssignment;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.database.Result;

/**
 * A model to represent an assignment with instances of assignments pending
 * manual marking.
 */
public class ModelAssignmentsList
{
    // Fields ******************************************************************
    private int         assID;
    private String      assTitle;
    private DateTime    assDue;
    private int         assPending;
    private int         moduleID;
    private String      moduleTitle;
    // Methods - Constructors **************************************************
    private ModelAssignmentsList(int assID, String assTitle, DateTime assDue, int assPending, int moduleID, String moduleTitle)
    {
        this.assID = assID;
        this.assTitle = assTitle;
        this.assDue = assDue;
        this.assPending = assPending;
        this.moduleID = moduleID;
        this.moduleTitle = moduleTitle;
    }
    // Methods - Persistence ***************************************************
    /**
     * @param conn Database connector.
     * @return Array of assignment-information requiring marking; can be empty
     * if no assignments require marking.
     */
    public static ModelAssignmentsList[] load(Connector conn)
    {
        try
        {
            Result res = conn.read("SELECT a.assid, m.moduleid, m.title AS m_title, a.title AS ass_title, a.due, COUNT(ai.*) AS pending FROM pals_assignment AS a LEFT OUTER JOIN pals_assignment_instance AS ai ON ai.assid=a.assid LEFT OUTER JOIN pals_modules AS m ON m.moduleid=a.moduleid WHERE ai.status=? GROUP BY a.assid, m.moduleid HAVING COUNT(ai.*) > 0 ORDER BY a.title ASC;", InstanceAssignment.Status.Submitted.getStatus());
            ArrayList<ModelAssignmentsList> buffer = new ArrayList<>();
            ModelAssignmentsList mam;
            while(res.next())
            {
                if((mam = load(res)) != null)
                    buffer.add(mam);
            }
            return buffer.toArray(new ModelAssignmentsList[buffer.size()]);
        }
        catch(DatabaseException ex)
        {
            return new ModelAssignmentsList[0];
        }
    }
    private static ModelAssignmentsList load(Result res)
    {
        try
        {
            Object t = res.get("due");
            return new ModelAssignmentsList((int)res.get("assid"), (String)res.get("ass_title"), t != null ? new DateTime(t) : null, (int)(long)res.get("pending"), (int)res.get("moduleid"), (String)res.get("m_title"));
        }
        catch(DatabaseException ex)
        {
            return null;
        }
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The assignment identifier.
     */
    public int getAssID()
    {
        return assID;
    }
    /**
     * @return The title of the assignment.
     */
    public String getAssTitle()
    {
        return assTitle;
    }
    /**
     * @return The date the assignment is due; can be null.
     */
    public DateTime getAssDue()
    {
        return assDue;
    }
    /**
     * @return The number of instances of assignments pending marking.
     */
    public int getAssPending()
    {
        return assPending;
    }
    /**
     * @return The identifier of the module of the assignment.
     */
    public int getModuleID()
    {
        return moduleID;
    }
    /**
     * @return The title of the module of the assignment.
     */
    public String getModuleTitle()
    {
        return moduleTitle;
    }
}
