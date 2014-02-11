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
