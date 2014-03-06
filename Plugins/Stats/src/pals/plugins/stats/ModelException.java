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
package pals.plugins.stats;

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
 * 
 * Used by the stats system.
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
            return load(conn.read("SELECT e.exdate, e.aqid, e.aiid, em.message, aq.page "+
                    "FROM pals_exceptions AS e "+
                    "LEFT OUTER JOIN pals_assignment_questions AS aq ON aq.aqid=e.aqid "+
                    "LEFT OUTER JOIN pals_exception_messages AS em ON em.emid=e.emid "+
                    "WHERE e.ecid=? ORDER BY e.exdate DESC LIMIT ? OFFSET ?;",
                    ecid, limit, offset));
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
            return load(conn.read(
                    "SELECT e.exdate, e.aqid, e.aiid, em.message, aq.page "+
                    "FROM pals_exceptions AS e "+
                    "LEFT OUTER JOIN pals_exception_classes AS ec ON e.ecid=ec.ecid "+
                    "LEFT OUTER JOIN pals_exception_messages AS em ON em.emid=e.emid "+
                    "LEFT OUTER JOIN pals_assignment_instance AS ai ON ai.aiid=e.aiid "+
                    "LEFT OUTER JOIN pals_assignment AS a ON a.assid=ai.assid "+
                    "LEFT OUTER JOIN pals_assignment_questions AS aq ON aq.aqid=e.aqid "+
                    "WHERE e.ecid=ec.ecid AND ec.ecid=? AND a.moduleid=? ORDER BY e.exdate DESC LIMIT ? OFFSET ?;",
                    ecid, module.getModuleID(), limit, offset));
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
            return load(conn.read(
                    "SELECT e.exdate, e.aqid, e.aiid, em.message, aq.page "+
                    "FROM pals_exceptions AS e "+
                    "LEFT OUTER JOIN pals_exception_classes AS ec ON e.ecid=ec.ecid "+
                    "LEFT OUTER JOIN pals_exception_messages AS em ON em.emid=e.emid "+
                    "LEFT OUTER JOIN pals_assignment_instance AS ai ON ai.aiid=e.aiid "+
                    "LEFT OUTER JOIN pals_assignment_questions AS aq ON aq.aqid=e.aqid "+
                    "WHERE e.ecid=ec.ecid AND ec.ecid=? AND ai.assid=? ORDER BY e.exdate DESC LIMIT ? OFFSET ?;",
                    ecid, ass.getAssID(), limit, offset));
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
            return load(conn.read(
                    "SELECT e.exdate, e.aqid, e.aiid, em.message, aq.page "+
                    "FROM pals_exceptions AS e "+
                    "LEFT OUTER JOIN pals_exception_classes AS ec ON e.ecid=ec.ecid "+
                    "LEFT OUTER JOIN pals_exception_messages AS em ON em.emid=e.emid "+
                    "LEFT OUTER JOIN pals_assignment_questions AS aq ON aq.aqid=e.aqid "+
                    "WHERE e.ecid=ec.ecid AND ec.ecid=? AND aq.qid=? ORDER BY e.exdate DESC LIMIT ? OFFSET ?;",
                    ecid, q.getQID(), limit, offset));
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
