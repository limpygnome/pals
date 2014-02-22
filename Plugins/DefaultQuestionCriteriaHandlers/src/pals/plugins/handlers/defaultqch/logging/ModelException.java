package pals.plugins.handlers.defaultqch.logging;

import org.joda.time.DateTime;
import pals.base.assessment.Assignment;
import pals.base.assessment.InstanceAssignmentQuestion;
import pals.base.assessment.Module;
import pals.base.assessment.Question;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.database.Result;

/**
 * A model used for creating and retrieving information regarding instances of
 * exceptions, from assignment-questions.
 */
public class ModelException
{
    // Fields ******************************************************************
    private int         aqid,
                        aiid;
    private String      className,
                        message;
    private DateTime    dt;
    private boolean     runtime;
    // Fields - Information ****************************************************
    private int aiidPage;
    // Methods - Constructors **************************************************
    public ModelException(String className, String message, InstanceAssignmentQuestion iaq, boolean runtime)
    {
        if(className == null || iaq == null)
            throw new IllegalArgumentException("ModelException ~ only message can be null!");
        this.className = className;
        this.message = message;
        this.aqid = iaq.getAssignmentQuestion().getAQID();
        this.aiid = iaq.getInstanceAssignment().getAIID();
        this.message = message;
        this.runtime = runtime;
    }
    // Methods - Persistence ***************************************************
    /**
     * Persists the instance; this only supports creating new instances. This
     * will not update a loaded model.
     * 
     * @param conn Database connector.
     * @return True = succeeded, false = failed.
     */
    public boolean persist(Connector conn)
    {
        try
        {
            Object t;
            int ecid, emid = -1;
            // Check the class exists
            t = conn.executeScalar("SELECT ecid FROM pals_exception_classes WHERE class_name=? AND runtime=?;", className, runtime ? "1" : "0");
            if(t == null)
                t = conn.executeScalar("INSERT INTO pals_exception_classes (class_name, runtime) VALUES(?, ?) RETURNING ecid;", className, runtime ? "1" : "0");
            ecid = (int)t;
            // Check the message exists
            if(message != null)
            {
                t = conn.executeScalar("SELECT emid FROM pals_exception_messages WHERE message=?;", message);
                if(t == null)
                    t = conn.executeScalar("INSERT INTO pals_exception_messages (message) VALUES(?) RETURNING emid;", message);
                emid = (int)t;
            }
            // Create instance
            conn.execute("INSERT INTO pals_exceptions (ecid, emid, aqid, aiid, exdate) VALUES(?,?,?,?,current_timestamp);", ecid, message != null ? emid : null, aqid, aiid);
        }
        catch(DatabaseException ex)
        {
            System.err.println(ex.getMessage());
            return false;
        }
        return true;
    }
    /**
     * @param conn Database connector.
     * @param ecid The identifier of the exception class.
     * @param lf The load filter applied.
     * @return Array of instances; can be empty.
     */
    public static ModelException[] load(Connector conn, int ecid, ModelExceptionClass.LoadRemoveFilter lf)
    {
        try
        {
            if(lf == ModelExceptionClass.LoadRemoveFilter.None)
                return load(conn, conn.read("SELECT e.exdate, e.aqid, e.aiid, em.message FROM pals_exceptions AS e WHERE e.ecid=?;", ecid));
            else
                return load(conn, conn.read("SELECT e.exdate, e.aqid, e.aiid, em.message FROM pals_exceptions AS e, pals_exception_classes AS ec WHERE ec.runtime=? AND ec.ecid=?;", lf == ModelExceptionClass.LoadRemoveFilter.FilterRuntime ? "1" : "0", ecid));
        }
        catch(DatabaseException ex)
        {
            return new ModelException[0];
        }
    }
    /**
     * @param conn Database connector.
     * @param ecid The identifier of the exception class.
     * @param module The filter for a module.
     * @param lf The load filter applied.
     * @return Array of instances; can be empty.
     */
    public static ModelException[] load(Connector conn, int ecid, Module module, ModelExceptionClass.LoadRemoveFilter lf)
    {
        try
        {
            if(lf == ModelExceptionClass.LoadRemoveFilter.None)
                return load(conn, conn.read("SELECT e.exdate, e.aqid, e.aiid, em.message FROM pals_exception_classes AS ec, pals_exceptions AS e, pals_exception_messages AS em, pals_assignment_instance AS ai, pals_assignment AS a WHERE e.ecid=ec.ecid AND em.emid=e.emid AND ai.aiid=e.aiid AND a.assid=ai.assid AND a.moduleid=? ORDER BY e.exdate DESC;", module.getModuleID()));
            else
                return load(conn, conn.read("SELECT e.exdate, e.aqid, e.aiid, em.message FROM pals_exception_classes AS ec, pals_exceptions AS e, pals_exception_messages AS em, pals_assignment_instance AS ai, pals_assignment AS a WHERE e.ecid=ec.ecid AND ec.runtime=? AND em.emid=e.emid AND ai.aiid=e.aiid AND a.assid=ai.assid AND a.moduleid=? ORDER BY e.exdate DESC;", lf == ModelExceptionClass.LoadRemoveFilter.FilterRuntime ? "1" : "0", module.getModuleID()));
        }
        catch(DatabaseException ex)
        {
            return new ModelException[0];
        }
    }
    /**
     * @param conn Database connector.
     * @param ecid The identifier of the exception class.
     * @param ass The filter for an assignment.
     * @param lf The load filter applied.
     * @return 
     */
    public static ModelException[] load(Connector conn, int ecid, Assignment ass, ModelExceptionClass.LoadRemoveFilter lf)
    {
        try
        {
            if(lf == ModelExceptionClass.LoadRemoveFilter.None)
                return load(conn, conn.read("SELECT e.exdate, e.aqid, e.aiid, em.message FROM pals_exception_classes AS ec, pals_exceptions AS e, pals_assignment_instance AS ai WHERE e.ecid=ec.ecid AND ai.aiid=e.aiid AND ai.assid=? ORDER BY e.exdate DESC;", ass.getAssID()));
            else
                return load(conn, conn.read("SELECT e.exdate, e.aqid, e.aiid, em.message FROM pals_exception_classes AS ec, pals_exceptions AS e, pals_assignment_instance AS ai WHERE e.ecid=ec.ecid AND ec.runtime=? AND ai.aiid=e.aiid AND ai.assid=? ORDER BY e.exdate DESC;", lf == ModelExceptionClass.LoadRemoveFilter.FilterRuntime ? "1" : "0", ass.getAssID()));
        }
        catch(DatabaseException ex)
        {
            return new ModelException[0];
        }
    }
    /**
     * @param conn Database connector.
     * @param ecid The identifier of the exception class.
     * @param q The filter for a question.
     * @param lf The load filter applied.
     * @return Array of instances; can be empty.
     */
    public static ModelException[] load(Connector conn, int ecid, Question q, ModelExceptionClass.LoadRemoveFilter lf)
    {
        try
        {
            if(lf == ModelExceptionClass.LoadRemoveFilter.None)
                return load(conn, conn.read("SELECT e.exdate, e.aqid, e.aiid, em.message FROM pals_exception_classes AS ec, pals_exceptions AS e, pals_assignment_questions AS aq WHERE e.ecid=ec.ecid AND aq.aqid=e.aqid AND aq.qid=? ORDER BY e.exdate DESC;", q.getQID()));
            else
                return load(conn, conn.read("SELECT e.exdate, e.aqid, e.aiid, em.message FROM pals_exception_classes AS ec, pals_exceptions AS e, pals_assignment_questions AS aq WHERE e.ecid=ec.ecid AND ec.runtime=? AND aq.aqid=e.aqid AND aq.qid=? ORDER BY e.exdate DESC;", lf == ModelExceptionClass.LoadRemoveFilter.FilterRuntime ? "1" : "0", q.getQID()));
        }
        catch(DatabaseException ex)
        {
            return new ModelException[0];
        }
    }
    /**
     * @param conn Database connector.
     * @param res Result data; next() should not be invoked.
     * @return Array of instances; can be empty.
     */
    public static ModelException[] load(Connector conn, Result res)
    {
        return null;
    }
    // Methods - Accessors *****************************************************
    public int getAQID()
    {
        return aqid;
    }
    public int getAIID()
    {
        return aiid;
    }
    public int getAIIDPage()
    {
        return aiidPage;
    }
    public String getClassName()
    {
        return className;
    }
    public String getMessage()
    {
        return message;
    }
    public DateTime getDateTime()
    {
        return dt;
    }
}
