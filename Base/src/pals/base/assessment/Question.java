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
package pals.base.assessment;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import pals.base.Logging;
import pals.base.NodeCore;
import pals.base.UUID;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.database.Result;
import pals.base.utils.Misc;

/**
 * A model which represents a question, of an assignment.
 */
public class Question
{
    // Enums *******************************************************************
    public enum PersistStatus
    {
        Failed,
        Failed_Serialize,
        Success,
        Invalid_QuestionType,
        Invalid_Title
    }
    // Fields ******************************************************************
    private int             qid;
    private TypeQuestion    qtype;
    private String          title;
    private Object          data;
    // Methods - Constructors **************************************************
    /**
     * Creates a new nullified unpersisted model.
     */
    public Question()
    {
        this(null, null, null);
    }
    /**
     * Creates a new unpersisted model.
     * 
     * @param qtype Type of question.
     * @param title Title of the question.
     * @param data The question's data.
     */
    public Question(TypeQuestion qtype, String title, Object data)
    {
        this.qid = -1;
        this.qtype = qtype;
        this.title = title;
        this.data = data;
    }
    // Methods - Persistence ***************************************************
    /**
     * Loads persisted models.
     * 
     * @param core Current instance of the core.
     * @param conn Database connector.
     * @param filter The title filter; can be null (to be ignored).
     * @param amount The number of models to retrieve at a time.
     * @param offset The number of models skipped.
     * @return Array of models; can be empty.
     */
    public static Question[] load(NodeCore core, Connector conn, String filter, int amount, int offset)
    {
        try
        {
            ArrayList<Question> buffer = new ArrayList<>();
            Result res;
            if(filter == null || filter.length() == 0)
                res = conn.read("SELECT * FROM pals_question ORDER BY title ASC LIMIT ? OFFSET ?;", amount, offset);
            else
                res = conn.read("SELECT * FROM pals_question WHERE title LIKE ? ORDER BY title ASC LIMIT ? OFFSET ?;", "%"+(filter.replace("%", ""))+"%", amount, offset);
            Question q;
            while(res.next())
            {
                if((q = load(core, conn, res)) != null)
                    buffer.add(q);
            }
            return buffer.toArray(new Question[buffer.size()]);
        }
        catch(DatabaseException ex)
        {
            core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return new Question[0];
        }
    }
    /**
     * Loads a persisted model by its identifier.
     * 
     * @param core Current instance of the core.
     * @param conn Database connector.
     * @param qid Question identifier.
     * @return Instance of model or null.
     */
    public static Question load(NodeCore core, Connector conn, int qid)
    {
        try
        {
            Result res = conn.read("SELECT * FROM pals_question WHERE qid=?;", qid);
            return res.next() ? load(core, conn, res) : null;
        }
        catch(DatabaseException ex)
        {
            core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return null;
        }
    }
    /**
     * Loads a persisted model from a result; next() should be pre-invoked.
     * 
     * @param core Current instance of the core.
     * @param conn Database connector.
     * @param result Database result.
     * @return Instance of model or null.
     */
    public static Question load(NodeCore core, Connector conn, Result result)
    {
        try
        {
            // Fetch qtype
            TypeQuestion tq = TypeQuestion.load(conn, UUID.parse((byte[])result.get("uuid_qtype")));
            if(tq == null)
                return null;
            // Read serialized object
            Object obj = Utils.loadData(core, result, "data");
            // Create and return instance
            Question q = new Question(tq, (String)result.get("title"), obj);
            q.qid = (int)result.get("qid");
            return q;
        }
        catch(DatabaseException ex)
        {
            core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return null;
        }
    }
    /**
     * Persists the model; for unpersisted models, the qid is updated with the
     * newly assigned identifier.
     * 
     * @param conn Database connector.
     * @return The status of the operation.
     */
    public PersistStatus persist(Connector conn)
    {
        try
        {
            // Validate data
            if(qtype == null)
                return PersistStatus.Invalid_QuestionType;
            else if(title.length() < getTitleMin() || title.length() > getTitleMax())
                return PersistStatus.Invalid_Title;
            else
            {
                byte[] bdata;
                // Serialize data field
                try
                {
                    bdata = Misc.bytesSerialize(data);
                }
                catch(IOException ex)
                {
                    return PersistStatus.Failed_Serialize;
                }
                // Persist data
                if(qid == -1)
                {
                    qid = (int)conn.executeScalar("INSERT INTO pals_question (uuid_qtype, title, data) VALUES(?,?,?) RETURNING qid;", qtype.getUuidQType().getBytes(), title, bdata);
                }
                else   
                {
                    conn.execute("UPDATE pals_question SET uuid_qtype=?, title=?, data=? WHERE qid=?;", qtype.getUuidQType().getBytes(), title, bdata, qid);
                }
                return PersistStatus.Success;
            }
        }
        catch(DatabaseException ex)
        {
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return PersistStatus.Failed;
        }
    }
    /**
     * Removes the model from the database.
     * 
     * @param conn Database connector.
     * @return True = removed, false = failed.
     */
    public boolean remove(Connector conn)
    {
        try
        {
            conn.execute("DELETE FROM pals_question WHERE qid=?;", qid);
            return true;
        }
        catch(DatabaseException ex)
        {
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return false;
        }
    }
    // Methods - Mutators ******************************************************
    /**
     * @param qtype The type of question.
     */
    public void setQtype(TypeQuestion qtype)
    {
        this.qtype = qtype;
    }
    /**
     * @param title The title of the question.
     */
    public void setTitle(String title)
    {
        this.title = title;
    }
    /**
     * @param data Serializable data, which can be used by a question-type to
     * render/handle the question.
     * @param <T> The type of data must be serializable.
     */
    public <T extends Serializable> void setData(T data)
    {
        this.data = data;
    }
    // Methods - Accessors *****************************************************
    /**
     * @return Indicates if the model has been persisted.
     */
    public boolean isPersisted()
    {
        return qid != -1;
    }
    /**
     * @return The identifier of this question.
     */
    public int getQID()
    {
        return qid;
    }
    /**
     * @return The type of question.
     */
    public TypeQuestion getQtype()
    {
        return qtype;
    }
    /**
     * @return The question's title.
     */
    public String getTitle()
    {
        return title;
    }
    /**
     * @return Data used by the question-type.
     * @param <T> The data-type of the object is serializable. 
     */
    public <T extends Serializable> T getData()
    {
        return (T)data;
    }
    /**
     * @param conn Database connector.
     * @return The number of assignments reliant on this question.
     */
    public int getDependentAssignments(Connector conn)
    {
        try
        {
            return (int)(long)conn.executeScalar("SELECT COUNT('') FROM pals_assignment_questions WHERE qid=?;", qid);
        }
        catch(DatabaseException ex)
        {
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return 0;
        }
    }
    // Methods - Accessors - Limits ********************************************
    /**
     * @return The minimum length of a question's title.
     */
    public int getTitleMin()
    {
        return 1;
    }
    /**
     * @return The maximum length of a question's title.
     */
    public int getTitleMax()
    {
        return 64;
    }
}
