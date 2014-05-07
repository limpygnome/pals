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
 * A criteria for marking a question.
 * 
 * @version 1.0
 */
public class QuestionCriteria
{
    // Enums *******************************************************************
    /**
     * The status from attempting to persist a model.
     * 
     * @since 1.0
     */
    public enum PersistStatus
    {
        /**
         * Successfully persisted.
         * 
         * @since 1.0
         */
        Success,
        /**
         * Failed to persist due to an exception or unknown state.
         * 
         * @since 1.0
         */
        Failed,
        /**
         * Failed to serialize data.
         * 
         * @since 1.0
         */
        Failed_Serialize,
        /**
         * Invalid question.
         * 
         * @since 1.0
         */
        Invalid_Question,
        /**
         * Invalid criteria.
         * 
         * @since 1.0
         */
        Invalid_Criteria,
        /**
         * Invalid weight.
         * 
         * @since 1.0
         */
        Invalid_Weight,
        /**
         * Invalid title.
         * 
         * @since 1.0
         */
        Invalid_Title;
        /**
         * Retrieves the text for the persist status.
         * 
         * @param qc The question-criteria model being persisted.
         * @return The web text associated with the operation.
         * @since 1.0
         */
        public String getText(QuestionCriteria qc)
        {
            switch(this)
            {
                default:
                case Failed:
                    return "Failed due to an unknown error; pelase try again or contact an administrator!";
                case Failed_Serialize:
                    return "Failed to serialize model; please try again or contact an administrator!";
                case Invalid_Criteria:
                    return "Invalid criteria-type.";
                case Invalid_Question:
                    return "Invalid question.";
                case Invalid_Title:
                    return "Invalid title; must be "+qc.getTitleMin()+" to "+qc.getTitleMax()+" characters in length!";
                case Invalid_Weight:
                    return "Invalid weight; must be numeric and greater than zero!";
                case Success:
                    return "Updated criteria successfully.";
            }
        }
    }
    // Fields ******************************************************************
    private int             qcid;       // Unique identifier for this model.
    private Question        question;   // The question to which this, criteria, belongs.
    private TypeCriteria    criteria;   // The type of criteria.
    private String          title;      // The title of the question's criteria.
    private Object          data;       // Any data stored by the criteria-type.
    private int             weight;     // Weight of criteria.
    // Methods - Constructors **************************************************
    /**
     * Creates a new unpersisted model.
     * 
     * @since 1.0
     */
    public QuestionCriteria()
    {
        this(null, null, null, null, 0);
    }
    /**
     * Creates a new unpersisted model.
     * 
     * @param question The question to which this, criteria, belongs.
     * @param criteria The criteria-type.
     * @param title The title of the criteria.
     * @param data Any data for the criteria-type.
     * @param weight The weight of the criteria.
     * @since 1.0
     */
    public QuestionCriteria(Question question, TypeCriteria criteria, String title, Object data, int weight)
    {
        this.qcid = -1;
        this.question = question;
        this.criteria = criteria;
        this.title = title;
        this.data = data;
        this.weight = weight;
    }
    // Methods - Persistence ***************************************************
    /**
     * Loads all of the criterias for a question.
     * 
     * @param core Current instance of the core.
     * @param conn Database connector.
     * @param q The question to which the criterias belong; cannot be null.
     * @return Array of question-criterias; can be empty.
     * @since 1.0
     */
    public static QuestionCriteria[] loadAll(NodeCore core, Connector conn, Question q)
    {
        try
        {
            ArrayList<QuestionCriteria> buffer = new ArrayList<>();
            // Load from the database, iterate each result and load the model
            Result res = conn.read("SELECT * FROM pals_question_criteria WHERE qid=? ORDER BY title ASC;", q.getQID());
            QuestionCriteria qc;
            while(res.next())
            {
                if((qc = load(core, conn, q, res)) != null)
                    buffer.add(qc);
            }
            return buffer.toArray(new QuestionCriteria[buffer.size()]);
        }
        catch(DatabaseException ex)
        {
            core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return new QuestionCriteria[0];
        }
    }
    /**
     * Loads a persisted model.
     * 
     * @param core Current instance of the core.
     * @param conn Database connector.
     * @param question The question to which this criteria belongs; if this is
     * null, the question model is loaded (could be expensive for multiple
     * criterias).
     * @param qcid The identifier of the criteria.
     * @return An instance of the model or null.
     * @since 1.0
     */
    public static QuestionCriteria load(NodeCore core, Connector conn, Question question, int qcid)
    {
        try
        {
            Result res = conn.read("SELECT * FROM pals_question_criteria WHERE qcid=?;", qcid);
            return res.next() ? load(core, conn, question, res) : null;
        }
        catch(DatabaseException ex)
        {
            core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return null;
        }
    }
    /**
     * Loads a persisted model.
     * 
     * @param core Current instance of the core.
     * @param conn Database connector.
     * @param question The question to which this criteria belongs; if this is
     * null, the question model is loaded (could be expensive for multiple
     * criterias).
     * @param result The result of a query, with the method next() pre-invoked.
     * @return An instance of the model or null.
     * @since 1.0
     */
    public static QuestionCriteria load(NodeCore core, Connector conn, Question question, Result result)
    {
        try
        {
            // Load criteria-type
            TypeCriteria tc = TypeCriteria.load(conn, UUID.parse((byte[])result.get("uuid_ctype")));
            if(tc == null)
                return null;
            // Load question, if null
            if(question == null)
            {
                question = Question.load(core, conn, (int)result.get("qid"));
            }
            // Deserialize data
            Object data = Utils.loadData(core, result, "data");
            // Create instance and return
            QuestionCriteria qc = new QuestionCriteria(question, tc, (String)result.get("title"), data, (int)result.get("weight"));
            qc.qcid = (int)result.get("qcid");
            return qc;
        }
        catch(DatabaseException ex)
        {
            core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return null;
        }
    }
    /**
     * Persists the model; if the model is unpersisted, it's assigned a new
     * identifier (qcid).
     * 
     * @param conn Database connector.
     * @return The status of the operation.
     * @since 1.0
     */
    public PersistStatus persist(Connector conn)
    {
        // Validate data
        if(question == null || !question.isPersisted())
            return PersistStatus.Invalid_Question;
        else if(criteria == null || !criteria.isPersisted())
            return PersistStatus.Invalid_Criteria;
        else if(weight <= 0)
            return PersistStatus.Invalid_Weight;
        else if(title == null || title.length() < getTitleMin() || title.length() > getTitleMax())
            return PersistStatus.Invalid_Title;
        else
        {
            // Serialize the data
            byte[] bdata;
            try
            {
                bdata = Misc.bytesSerialize(data);
            }
            catch(IOException ex)
            {
                return PersistStatus.Failed_Serialize;
            }
            // Persist the model
            try
            {
                if(qcid == -1)
                {
                    qcid = (int)conn.executeScalar("INSERT INTO pals_question_criteria (qid, uuid_ctype, title, data, weight) VALUES(?,?,?,?,?) RETURNING qcid;",
                            question.getQID(),
                            criteria.getUuidCType().getBytes(),
                            title,
                            bdata,
                            weight
                            );
                }
                else
                {
                    conn.execute("UPDATE pals_question_criteria SET qid=?, uuid_ctype=?, title=?, data=?, weight=? WHERE qcid=?;",
                            question.getQID(),
                            criteria.getUuidCType().getBytes(),
                            title,
                            bdata,
                            weight,
                            qcid
                            );
                }
                return PersistStatus.Success;
            }
            catch(DatabaseException ex)
            {
                NodeCore core;
                if((core = NodeCore.getInstance())!=null)
                    core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
                return PersistStatus.Failed;
            }
        }
    }
    /**
     * Unpersists the criteria.
     * 
     * @param conn Database connector.
     * @return True = removed, false = failed.
     * @since 1.0
     */
    public boolean delete(Connector conn)
    {
        if(qcid == -1)
            return false;
        try
        {
            conn.execute("DELETE FROM pals_question_criteria WHERE qcid=?;", qcid);
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
     * Sets the question.
     * 
     * @param question The question to which this criteria belongs.
     * @since 1.0
     */
    public void setQuestion(Question question)
    {
        this.question = question;
    }
    /**
     * Sets the criteria.
     * 
     * @param criteria The type of criteria.
     * @since 1.0
     */
    public void setCriteria(TypeCriteria criteria)
    {
        this.criteria = criteria;
    }
    /**
     * Sets the title.
     * 
     * @param title The new title for the model.
     * @since 1.0
     */
    public void setTitle(String title)
    {
        this.title = title;
    }
    /**
     * Sets the data.
     * 
     * @param data Any data, which can be serialized, set by the criteria-type.
     * @param <T> Serializable data-type.
     * @since 1.0
     */
    public <T extends Serializable> void  setData(T data)
    {
        this.data = data;
    }
    /**
     * Sets the weight, relative to the question.
     * 
     * @param weight The weight of this criteria; can be any positive arbitrary
     * number.
     * @since 1.0
     */
    public void setWeight(int weight)
    {
        this.weight = weight;
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
        return qcid != -1;
    }
    /**
     * The identifier.
     * 
     * @return The identifier of this criteria.
     * @since 1.0
     */
    public int getQCID()
    {
        return qcid;
    }
    /**
     * The question.
     * 
     * @return The question to which this, criteria, belongs.
     * @since 1.0
     */
    public Question getQuestion()
    {
        return question;
    }
    /**
     * The criteria type.
     * 
     * @return The type of criteria.
     * @since 1.0
     */
    public TypeCriteria getCriteria()
    {
        return criteria;
    }
    /**
     * The title.
     * 
     * @return The title of the question-criteria.
     * @since 1.0
     */
    public String getTitle()
    {
        return title;
    }
    /**
     * The serializable data.
     * 
     * @return The custom data set by the criteria-type.
     * @since 1.0
     */
    public Object getData()
    {
        return data;
    }
    /**
     * The weight, relative to the question.
     * 
     * @return The weight of the criteria.
     * @since 1.0
     */
    public int getWeight()
    {
        return weight;
    }
    /**
     * The minimum length of the title.
     * 
     * @return The minimum length of the title.
     * @since 1.0
     */
    public int getTitleMin()
    {
        return 1;
    }
    /**
     * The maximum length of the title.
     * 
     * @return The maximum length of the title.
     * @since 1.0
     */
    public int getTitleMax()
    {
        return 64;
    }
    // Methods - Overrides *****************************************************
    /**
     * Tests if the specified object is equal to the current instance, based
     * on being the same type and having the same identifier.
     * 
     * @param o The object to be tested.
     * @return True = equal, false = not equal.
     * @since 1.0
     */
    @Override
    public boolean equals(Object o)
    {
        if(o == null || !(o instanceof QuestionCriteria))
            return false;
        QuestionCriteria qc = (QuestionCriteria)o;
        return qc.qcid == qcid;
    }
    /**
     * The hash-code, based on the question.
     * 
     * @return The hash-code.
     * @since 1.0
     */
    @Override
    public int hashCode()
    {
        return question != null ? question.getQID() : -1;
    }
}
