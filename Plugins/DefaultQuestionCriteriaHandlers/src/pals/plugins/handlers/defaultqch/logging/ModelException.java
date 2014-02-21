package pals.plugins.handlers.defaultqch.logging;

import org.joda.time.DateTime;
import pals.base.assessment.Assignment;
import pals.base.assessment.InstanceAssignmentQuestion;
import pals.base.assessment.Module;
import pals.base.assessment.Question;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;

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
    public static ModelException[] load(Connector conn, Module module)
    {
        return null;
    }
    public static ModelException[] load(Connector conn, Assignment ass)
    {
        return null;
    }
    public static ModelException[] load(Connector conn, Question q)
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
