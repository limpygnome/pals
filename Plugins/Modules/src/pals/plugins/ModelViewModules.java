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
