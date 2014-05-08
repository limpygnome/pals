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
 * 
 * @version 1.0
 */
public class Question
{
    // Enums *******************************************************************
    /**
     * The status from attempting to persist the model.
     * 
     * @since 1.0
     */
    public enum PersistStatus
    {
        /**
         * Failed to persist due to an exception or unknown state.
         * 
         * @since 1.0
         */
        Failed,
        /**
         * Failed to serialize the question data.
         * 
         * @since 1.0
         */
        Failed_Serialize,
        /**
         * Successfully persisted.
         * 
         * @since 1.0
         */
        Success,
        /**
         * Invalid question type.
         * 
         * @since 1.0
         */
        Invalid_QuestionType,
        /**
         * Invalid title.
         * 
         * @since 1.0
         */
        Invalid_Title;
        
        public String getText(Question q)
        {
            switch(this)
            {
                default:
                case Failed:
                     return "Failed due to an unknown error; pelase try again or contact an administrator!";
                case Failed_Serialize:
                    return "Failed to serialize model; please try again or contact an administrator!";
                case Invalid_QuestionType:
                    return "Invalid question-type.";
                case Invalid_Title:
                    return "Title must be "+q.getTitleMin()+" to "+q.getTitleMax()+" characters in length!";
                case Success:
                    return "Updated question successfully.";
            }
        }
    }
    // Fields ******************************************************************
    private int             qid;
    private TypeQuestion    qtype;
    private String          title,
                            description;
    private Object          data;
    // Methods - Constructors **************************************************
    /**
     * Creates a new nullified unpersisted model.
     * 
     * @since 1.0
     */
    public Question()
    {
        this(null, null, null, null);
    }
    /**
     * Creates a new unpersisted model.
     * 
     * @param qtype Type of question.
     * @param title Title of the question.
     * @param description The description of the question.
     * @param data The question's data.
     * @since 1.0
     */
    public Question(TypeQuestion qtype, String title, String description, Object data)
    {
        this.qid = -1;
        this.qtype = qtype;
        this.title = title;
        this.description = description;
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
     * @since 1.0
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
                res = conn.read("SELECT * FROM pals_question WHERE title ILIKE ? ORDER BY title ASC LIMIT ? OFFSET ?;", "%"+(filter.replace("%", ""))+"%", amount, offset);
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
     * @since 1.0
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
     * @since 1.0
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
            Question q = new Question(tq, (String)result.get("title"), (String)result.get("description"), obj);
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
     * @since 1.0
     */
    public PersistStatus persist(Connector conn)
    {
        try
        {
            // Validate data
            if(qtype == null || !qtype.isPersisted() || qtype.getUuidQType() == null)
                return PersistStatus.Invalid_QuestionType;
            else if(title == null || title.length() < getTitleMin() || title.length() > getTitleMax())
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
                    qid = (int)conn.executeScalar("INSERT INTO pals_question (uuid_qtype, title, description, data) VALUES(?,?,?,?) RETURNING qid;", qtype.getUuidQType().getBytes(), title, description, bdata);
                }
                else   
                {
                    conn.execute("UPDATE pals_question SET uuid_qtype=?, title=?, description=?, data=? WHERE qid=?;", qtype.getUuidQType().getBytes(), title, description, bdata, qid);
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
     * @since 1.0
     */
    public boolean delete(Connector conn)
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
     * Sets the type of question.
     * 
     * @param qtype The type of question.
     * @since 1.0
     */
    public void setQtype(TypeQuestion qtype)
    {
        this.qtype = qtype;
    }
    /**
     * Sets the title.
     * 
     * @param title The title of the question.
     * @since 1.0
     */
    public void setTitle(String title)
    {
        this.title = title;
    }
    /**
     * Sets the description.
     * 
     * @param description Sets the description of this question.
     * @since 1.0
     */
    public void setDescription(String description)
    {
        this.description = description;
    }
    /**
     * Sets the data.
     * 
     * @param data Serializable data, which can be used by a question-type to
     * render/handle the question.
     * @param <T> The type of data must be serializable.
     * @since 1.0
     */
    public <T extends Serializable> void setData(T data)
    {
        this.data = data;
    }
    // Methods - Accessors *****************************************************
    /**
     * Indicates if the model has been persisted.
     * 
     * @return True = persisted, false = not persisted.
     * @since 1.0
     */
    public boolean isPersisted()
    {
        return qid != -1;
    }
    /**
     * Retrieves the identifier of this question.
     * 
     * @return The identifier.
     * @since 1.0
     */
    public int getQID()
    {
        return qid;
    }
    /**
     * Retrieves the type of question.
     * 
     * @return The type of question.
     * @since 1.0
     */
    public TypeQuestion getQtype()
    {
        return qtype;
    }
    /**
     * Retrieves the title of the question.
     * 
     * @return The title.
     * @since 1.0
     */
    public String getTitle()
    {
        return title;
    }
    /**
     * Retrieves the description of the question.
     * 
     * @return A description of the question; can be null or empty.
     */
    public String getDescription()
    {
        return description;
    }
    /**
     * The serialized data.
     * 
     * @return Data used by the question-type.
     * @param <T> The data-type of the object is serializable.
     * @since 1.0
     */
    public <T extends Serializable> T getData()
    {
        return (T)data;
    }
    /**
     * Fetches the number of assignment questions dependent on this question.
     * 
     * @param conn Database connector.
     * @return The number of assignments reliant on this question.
     * @since 1.0
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
     * The minimum length of a title.
     * 
     * @return The minimum length of a question's title.
     * @since 1.0
     */
    public int getTitleMin()
    {
        return 1;
    }
    /**
     * The maximum length of a title.
     * 
     * @return The maximum length of a question's title.
     * @since 1.0
     */
    public int getTitleMax()
    {
        return 64;
    }

    // Methods - Overrides *****************************************************
    /**
     * Tests if an object is equal to this instance, based on being the same
     * type and having the same qid identifier.
     * 
     * @param o The object to be tested.
     * @return True = same, false = not the smae.
     * @since 1.0
     */
    @Override
    public boolean equals(Object o)
    {
        if(o == null)
            return false;
        else if(!(o instanceof Question))
            return false;
        Question q = (Question)o;
        return q.qid == qid;
    }
    /**
     * The hash-code, derived from the hash-code of the question-type.
     * 
     * @return Hash code.
     * @since 1.0
     */
    @Override
    public int hashCode()
    {
        return qtype == null ? -1 : qtype.getUuidQType().hashCode();
    }
}
