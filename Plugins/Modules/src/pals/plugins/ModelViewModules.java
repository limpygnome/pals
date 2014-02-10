package pals.plugins;

import java.util.ArrayList;
import pals.base.assessment.InstanceAssignment;
import pals.base.auth.User;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.database.Result;

/**
 * A model with all the information regarding assignment-counts for modules.
 */
public class ModelViewModules
{
    // Fields ******************************************************************
    private int     moduleID;
    private String  title;
    private long    total,
                    unanswered,
                    answered,
                    incomplete;
    // Methods - Constructors **************************************************
    public ModelViewModules(int moduleID, String title, long total, long unanswered, long answered, long incomplete)
    {
        this.moduleID = moduleID;
        this.title = title;
        this.total = total;
        this.unanswered = unanswered;
        this.answered = answered;
        this.incomplete = incomplete;
    }
    // Methods - Persistence ***************************************************
    /**
     * @param conn Database connector.
     * @param user Current user.
     * @return List of modules and information regarding module assignments;
     * can be empty list.
     */
    public static ModelViewModules[] load(Connector conn, User user)
    {
        try
        {
            Result res = conn.read(
                "SELECT	m.moduleid," +
                "	m.title," +
                "	(SELECT COUNT('') FROM pals_assignment WHERE active='1' AND moduleid=m.moduleid) AS total," +
                "	(SELECT COUNT('') FROM pals_assignment AS a WHERE active='1' AND moduleid=m.moduleid AND NOT EXISTS(SELECT assid FROM pals_assignment_instance WHERE userid=me.userid AND assid=a.assid)) AS unanswered," +
                "	(SELECT COUNT('') FROM pals_assignment AS a WHERE active='1' AND moduleid=m.moduleid AND EXISTS(SELECT assid FROM pals_assignment_instance WHERE userid=me.userid AND assid=a.assid AND NOT status=?)) AS answered," +
                "	(SELECT COUNT('') FROM pals_assignment AS a WHERE active='1' AND moduleid=m.moduleid AND EXISTS(SELECT assid FROM pals_assignment_instance WHERE userid=me.userid AND assid=a.assid AND status=?)) AS incomplete" +
                " FROM pals_modules AS m, pals_modules_enrollment AS me WHERE m.moduleid=me.moduleid AND me.userid=?;",
                    InstanceAssignment.Status.Active.getStatus(),
                    InstanceAssignment.Status.Active.getStatus(),
                    user.getUserID()
            );
            ArrayList<ModelViewModules> buffer = new ArrayList<>();
            ModelViewModules t;
            while(res.next())
            {
                if((t = load(res)) != null)
                    buffer.add(t);
            }
            return buffer.toArray(new ModelViewModules[buffer.size()]);
        }
        catch(DatabaseException ex)
        {
            return new ModelViewModules[0];
        }
    }
    private static ModelViewModules load(Result res)
    {
        try
        {
            return new ModelViewModules((int)res.get("moduleid"), (String)res.get("title"), (long)res.get("total"), (long)res.get("unanswered"), (long)res.get("answered"), (long)res.get("incomplete"));
        }
        catch(DatabaseException ex)
        {
            return null;
        }
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The identifier of the module.
     */
    public int getModuleID()
    {
        return moduleID;
    }
    /**
     * @return The title of the module.
     */
    public String getTitle()
    {
        return title;
    }
    /**
     * @return The total assignments for the module.
     */
    public long getTotal()
    {
        return total;
    }
    /**
     * @return Total assignments unanswered.
     */
    public long getUnanswered()
    {
        return unanswered;
    }
    /**
     * @return Total assignments answered.
     */
    public long getAnswered()
    {
        return answered;
    }
    /**
     * @return Total assignments incomplete.
     */
    public long getIncomplete()
    {
        return incomplete;
    }
    
}
