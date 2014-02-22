package pals.plugins.handlers.defaultqch.logging;

import java.util.ArrayList;
import java.util.Date;
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
    private ModelException(int aqid, int aiid, String message, DateTime dt, int aiidPage)
    {
        this.aqid = aqid;
        this.aiid = aiid;
        this.className = null;
        this.message = message;
        this.dt = dt;
        this.aiidPage = aiidPage;
        this.runtime = false;
    }
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
        this.aiidPage = -1;
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
            return false;
        }
        return true;
    }
    /**
     * @param conn Database connector.
     * @param ecid The identifier of the exception class.
     * @param limit The limit applied to the query.
     * @param offset The offset applied to the query.
     * @return Array of instances; can be empty.
     */
    public static ModelException[] load(Connector conn, int ecid, int limit, int offset)
    {
        try
        {
            return load(conn.read("SELECT e.exdate, e.aqid, e.aiid, em.message, aq.page FROM pals_exceptions AS e, pals_exception_messages AS em, pals_assignment_questions AS aq WHERE e.ecid=? AND em.emid=e.emid AND aq.aqid=e.aqid ORDER BY e.exdate DESC LIMIT ? OFFSET ?;", ecid, limit, offset));
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
     * @param limit The limit applied to the query.
     * @param offset The offset applied to the query.
     * @return Array of instances; can be empty.
     */
    public static ModelException[] load(Connector conn, int ecid, Module module, int limit, int offset)
    {
        try
        {
            return load(conn.read("SELECT e.exdate, e.aqid, e.aiid, em.message, aq.page FROM pals_exception_classes AS ec, pals_exceptions AS e, pals_exception_messages AS em, pals_assignment_instance AS ai, pals_assignment AS a, pals_assignment_questions AS aq WHERE ec.ecid=? AND e.ecid=ec.ecid AND aq.aqid=e.aqid AND em.emid=e.emid AND ai.aiid=e.aiid AND a.assid=ai.assid AND a.moduleid=? ORDER BY e.exdate DESC LIMIT ? OFFSET ?;", ecid, module.getModuleID(), limit, offset));
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
     * @param limit The limit applied to the query.
     * @param offset The offset applied to the query.
     * @return Array of instances; can be empty.
     */
    public static ModelException[] load(Connector conn, int ecid, Assignment ass, int limit, int offset)
    {
        try
        {
            return load(conn.read("SELECT e.exdate, e.aqid, e.aiid, em.message, aq.page FROM pals_exception_classes AS ec, pals_exceptions AS e, pals_assignment_instance AS ai, pals_exception_messages AS em, pals_assignment_questions AS aq WHERE ec.ecid=? AND e.ecid=ec.ecid AND aq.aqid=e.aqid AND em.emid=e.emid AND ai.aiid=e.aiid AND ai.assid=? ORDER BY e.exdate DESC LIMIT ? OFFSET ?;", ecid, ass.getAssID(), limit, offset));
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
     * @param limit The limit applied to the query.
     * @param offset The offset applied to the query.
     * @return Array of instances; can be empty.
     */
    public static ModelException[] load(Connector conn, int ecid, Question q, int limit, int offset)
    {
        try
        {
            return load(conn.read("SELECT e.exdate, e.aqid, e.aiid, em.message, aq.page FROM pals_exception_classes AS ec, pals_exceptions AS e, pals_exception_messages AS em, pals_assignment_questions AS aq WHERE ec.ecid=? AND em.emid=e.emid AND e.ecid=ec.ecid AND aq.aqid=e.aqid AND aq.qid=? ORDER BY e.exdate DESC LIMIT ? OFFSET ?;", ecid, q.getQID(), limit, offset));
        }
        catch(DatabaseException ex)
        {
            return new ModelException[0];
        }
    }
    /**
     * @param res Result data; next() should not be invoked.
     * @return Array of instances; can be empty.
     */
    public static ModelException[] load(Result res)
    {
        try
        {
            ArrayList<ModelException> buffer = new ArrayList<>();
            ModelException t;
            while(res.next())
            {
                if((t = loadSingle(res)) != null)
                    buffer.add(t);
            }
            return buffer.toArray(new ModelException[buffer.size()]);
        }
        catch(DatabaseException ex)
        {
            return new ModelException[0];
        }
    }
    /**
     * @param res Result data; next() should be invoked.
     * @return Instance of model or null.
     */
    public static ModelException loadSingle(Result res)
    {
        try
        {
            return new ModelException((int)res.get("aqid"), (int)res.get("aiid"), (String)res.get("message"), new DateTime(((Date)res.get("exdate")).getTime()), (int)res.get("page"));
        }
        catch(DatabaseException ex)
        {
            return null;
        }
    }
    /**
     * Deletes all the data for the specified class.
     * 
     * @param conn Database connector.
     * @param ecid The identifier of the class.
     */
    public static void delete(Connector conn, int ecid)
    {
        try
        {
            conn.execute("DELETE FROM pals_exceptions WHERE exid IN (SELECT e.exid FROM pals_exceptions AS e, pals_exception_messages AS em WHERE e.ecid=? AND em.emid=e.emid);", ecid);
        }
        catch(DatabaseException ex)
        {
        }
    }
    /**
     * Deletes all the data for the specified class, filtered by module.
     * 
     * @param conn Database connector.
     * @param ecid The identifier of the class.
     * @param module The module filter.
     */
    public static void delete(Connector conn, int ecid, Module module)
    {
        try
        {
            conn.execute("DELETE FROM pals_exceptions WHERE exid IN (SELECT e.exid FROM pals_exception_classes AS ec, pals_exceptions AS e, pals_exception_messages AS em, pals_assignment_instance AS ai, pals_assignment AS a WHERE ec.ecid=? AND e.ecid=ec.ecid AND em.emid=e.emid AND ai.aiid=e.aiid AND a.assid=ai.assid AND a.moduleid=?);", ecid, module.getModuleID());
        }
        catch(DatabaseException ex)
        {
        }
    }
    /**
     * Deletes all the data for the specified class, filtered by assignment.
     * 
     * @param conn Database connector.
     * @param ecid The identifier of the class.
     * @param ass The assignment filter.
     */
    public static void delete(Connector conn, int ecid, Assignment ass)
    {
        try
        {
            conn.execute("DELETE FROM pals_exceptions WHERE exid IN (SELECT e.exid FROM pals_exception_classes AS ec, pals_exceptions AS e, pals_assignment_instance AS ai, pals_exception_messages AS em WHERE ec.ecid=? AND e.ecid=ec.ecid AND em.emid=e.emid AND ai.aiid=e.aiid AND ai.assid=?);", ecid, ass.getAssID());
        }
        catch(DatabaseException ex)
        {
        }
    }
    /**
     * Deletes all the data for the specified class, filtered by question.
     * 
     * @param conn Database connector.
     * @param ecid The identifier of the class.
     * @param q The question filter.
     */
    public static void delete(Connector conn, int ecid, Question q)
    {
        try
        {
            conn.execute("DELETE FROM pals_exceptions WHERE exid IN (SELECT e.exid FROM pals_exception_classes AS ec, pals_exceptions AS e, pals_exception_messages AS em, pals_assignment_questions AS aq WHERE ec.ecid=? AND em.emid=e.emid AND e.ecid=ec.ecid AND aq.aqid=e.aqid AND aq.qid=?);", ecid, q.getQID());
        }
        catch(DatabaseException ex)
        {
        }
    }
    // Methods - Accessors *****************************************************
    /**
     * @return Gets the assignment question identifier.
     */
    public int getAQID()
    {
        return aqid;
    }
    /**
     * @return Gets the identifier of the assignment instance.
     */
    public int getAIID()
    {
        return aiid;
    }
    /**
     * @return Gets the page of the assignment instance, on which the question
     * resides.
     */
    public int getAIIDPage()
    {
        return aiidPage;
    }
    /**
     * @return The name of the type of error.
     */
    public String getClassName()
    {
        return className;
    }
    /**
     * @return The message with the error; can be null.
     */
    public String getMessage()
    {
        return message;
    }
    /**
     * @return The date and time of when the error occurred.
     */
    public DateTime getDateTime()
    {
        return dt;
    }
}
