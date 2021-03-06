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
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.database.Result;
import pals.base.utils.Misc;

/**
 * A model which represents an instance of an assignment question, by a user.
 * 
 * @version 1.0
 */
public class InstanceAssignmentQuestion
{
    // Enums *******************************************************************
    /**
     * The status from persisting the model.
     * 
     * @since 1.0
     */
    public enum PersistStatus
    {
        /**
         * Successfully persisted model.
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
         * Failed to serialize the data.
         * 
         * @since 1.0
         */
        Failed_Serialize,
        /**
         * Invalid assignment question.
         * 
         * @since 1.0
         */
        Invalid_AssignmentQuestion,
        /**
         * Invalid instance of assignment.
         * 
         * @since 1.0
         */
        Invalid_InstanceAssignment
    }
    // Fields ******************************************************************
    private int                 aiqid;                  // The assignment-instance question identifier.
    private AssignmentQuestion  aq;                     // The assignment question.
    private InstanceAssignment  ia;                     // The current instance of the assignment.
    private Object              data;                   // Data for the current instance of the question.
    private boolean             answered;               // Indicates of the question has been answered.
    private double              mark;                   // The mark assigned to the question.
    // Fields - Cache **********************************************************
    private InstanceAssignmentCriteria[] cacheCriteria; // Cached models of the instances of criteria for this question.
    // Methods - Constructors **************************************************
    /**
     * Constructs a new instance of an assignment-instance question.
     * 
     * @since 1.0
     */
    public InstanceAssignmentQuestion()
    {
        this(null, null, null, false, 0);
    }
    /**
     * Constructs a new instance of an assignment-instance question.
     * 
     * @param aq The assignment question to instantiate.
     * @param ia The instance of the assignment.
     * @param data The data for the question, provided by a question-type.
     * @param answered Indicates if the question has been answered.
     * @param mark The mark of the question, between 0 to 100.
     * @since 1.0
     */
    public InstanceAssignmentQuestion(AssignmentQuestion aq, InstanceAssignment ia, Object data, boolean answered, double mark)
    {
        this.aiqid = -1;
        this.aq = aq;
        this.ia = ia;
        this.data = data;
        this.answered = answered;
        this.mark = mark;
    }
    // Methods - Persistence ***************************************************
    /**
     * Loads all instance of questions for an instance of an assignment.
     * 
     * @param core Current instance of the core.
     * @param conn Database connector.
     * @param ia Instance of the assignment; cannot be null.
     * @return Array of models; can be empty.
     * @since 1.0
     */
    public static InstanceAssignmentQuestion[] loadAll(NodeCore core, Connector conn, InstanceAssignment ia)
    {
        try
        {
            Result res = conn.read("SELECT * FROM pals_assignment_instance_question WHERE aiid=?;", ia.getAIID());
            ArrayList<InstanceAssignmentQuestion> buffer = new ArrayList<>();
            InstanceAssignmentQuestion iaq;
            while(res.next())
            {
                if((iaq = load(core, conn, ia, res)) != null)
                    buffer.add(iaq);
            }
            return buffer.toArray(new InstanceAssignmentQuestion[buffer.size()]);
        }
        catch(DatabaseException ex)
        {
            core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return new InstanceAssignmentQuestion[0];
        }
    }
    /***
     * Loads an instance-question for a question for an instance of an assignment.
     * 
     * @param core Current instance of the core.
     * @param conn Database connector.
     * @param ia Instance of the assignment; cannot be null.
     * @param aq Assignment-question; cannot be null.
     * @return An instance of the model or null.
     * @since 1.0
     */
    public static InstanceAssignmentQuestion load(NodeCore core, Connector conn, InstanceAssignment ia, AssignmentQuestion aq)
    {
        try
        {
            Result res = conn.read("SELECT * FROM pals_assignment_instance_question WHERE aqid=? AND aiid=?", aq.getAQID(), ia.getAIID());
            return res.next() ? load(core, conn, ia, res) : null;
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
     * @param ia The instance of the assignment (if known); can be null to be
     * loaded automatically (possibly expensive).
     * @param aiqid The identifier of the model to load.
     * @return An instance of the model or null.
     * @since 1.0
     */
    public static InstanceAssignmentQuestion load(NodeCore core, Connector conn, InstanceAssignment ia, int aiqid)
    {
        try
        {
            Result res;
            if(ia == null)
                res = conn.read("SELECT * FROM pals_assignment_instance_question WHERE aiqid=?;", aiqid);
            else
                res = conn.read("SELECT * FROM pals_assignment_instance_question WHERE aiqid=? AND aiid=?;", aiqid, ia.getAIID());
            return res.next() ? load(core, conn, ia, res) : null;
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
     * @param ia The instance of the assignment (if known); can be null to be
     * loaded automatically.
     * @param res The result from a query of data, with the method next()
     * pre-invoked.
     * @return An instance of the model or null.
     * @since 1.0
     */
    public static InstanceAssignmentQuestion load(NodeCore core, Connector conn, InstanceAssignment ia, Result res)
    {
        try
        {
            // Deserialize data
            Object data = Utils.loadData(core, res, "qdata");
            // Load instance of assignment, if null
            if(ia == null)
            {
                ia = InstanceAssignment.load(conn, null, null, (int)res.get("aiid"));
                if(ia == null)
                    return null;
            }
            // Load assignment question
            AssignmentQuestion aq = AssignmentQuestion.load(core, conn, ia.getAss(), (int)res.get("aqid"));
            if(aq == null)
                return null;
            InstanceAssignmentQuestion iaq = new InstanceAssignmentQuestion(aq, ia, data, ((String)res.get("answered")).equals("1"), (double)res.get("mark"));
            iaq.aiqid = (int)res.get("aiqid");
            return iaq;
        }
        catch(DatabaseException ex)
        {
            core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return null;
        }
    }
    /**
     * Persists the model to the database.
     * 
     * @param conn Database connector.
     * @return The status of the operation.
     * @since 1.0
     */
    public PersistStatus persist(Connector conn)
    {
        // Validate data
        if(aq == null)
            return PersistStatus.Invalid_AssignmentQuestion;
        else if(ia == null)
            return PersistStatus.Invalid_InstanceAssignment;
        else
        {
            // Serialize data
            byte[] bdata;
            try
            {
                bdata = Misc.bytesSerialize(data);
            }
            catch(IOException ex)
            {
                return PersistStatus.Failed_Serialize;
            }
            // Attempt to persist the data
            try
            {
                if(aiqid == -1)
                {
                    aiqid = (int)conn.executeScalar("INSERT INTO pals_assignment_instance_question (aqid, aiid, qdata, answered, mark) VALUES(?,?,?,?,?) RETURNING aiqid;",
                            aq.getAQID(),
                            ia.getAIID(),
                            bdata,
                            answered ? "1" : "0",
                            mark
                            );
                }
                else
                {
                    conn.execute("UPDATE pals_assignment_instance_question SET aqid=?, aiid=?, qdata=?, answered=?, mark=? WHERE aiqid=?;",
                            aq.getAQID(),
                            ia.getAIID(),
                            bdata,
                            answered ? "1" : "0",
                            mark,
                            aiqid
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
     * Unpersists the model from the database.
     * 
     * @param conn Database connector.
     * @return True = removed, false = failed.
     * @since 1.0
     */
    public boolean delete(Connector conn)
    {
        try
        {
            conn.execute("DELETE FROM pals_assignment_instance_question WHERE aiqid=?", aiqid);
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
     * Sets the assignment question.
     * 
     * @param aq Sets the assignment-question being instantiated.
     * @since 1.0
     */
    public void setAssignmentQuestion(AssignmentQuestion aq)
    {
        this.aq = aq;
    }
    /**
     * Sets the instance of assignment.
     * 
     * @param ia Sets the assignment instantiation.
     * @since 1.0
     */
    public void setInstanceAssignment(InstanceAssignment ia)
    {
        this.ia = ia;
    }
    /**
     * Sets the data for this instance of assignment question.
     * 
     * @param <T> The type must be serializable.
     * @param data Sets the data for this instance of the assignment-question.
     * @since 1.0
     */
    public <T extends Serializable> void setData(T data)
    {
        this.data = data;
    }
    /**
     * Sets if this instance has been answered.
     * 
     * @param answered Sets if this question has been answered.
     * @since 1.0
     */
    public void setAnswered(boolean answered)
    {
        this.answered = answered;
    }
    /**
     * Sets the mark for the question; must be 0 to 100.
     * 
     * @param mark The mark of the question, between (inclusively) 0 to 100.
     * @since 1.0
     */
    public void setMark(double mark)
    {
        if(mark >= 0.0 && mark <= 100.0)
            this.mark = mark;
    }
    // Methods - Accessors *****************************************************
    /**
     * Indicates if the current model is persisted.
     * 
     * @return True = persisted, false = not persisted.
     * @since 1.0
     */
    public boolean isPersisted()
    {
        return aiqid != -1;
    }
    /**
     * The unique identifier of this model.
     * 
     * @return The identifier.
     * @since 1.0
     */
    public int getAIQID()
    {
        return aiqid;
    }
    /**
     * The assignment-question being instantiated.
     * 
     * @return The assignment-question model.
     * @since 1.0
     */
    public AssignmentQuestion getAssignmentQuestion()
    {
        return aq;
    }
    /**
     * The assignment being instantiation.
     * 
     * @return The assignment model.
     * @since 1.0
     */
    public InstanceAssignment getInstanceAssignment()
    {
        return ia;
    }
    /**
     * The data for this instance of assignment question.
     * 
     * @return Data; serializable.
     * @since 1.0
     */
    public Object getData()
    {
        return data;
    }
    /**
     * Indicates if the question has been answered. Useful for speeding-up
     * the marking process and indicating unanswered questions before
     * a user submits an instance of an assignment.
     * 
     * @return True = answered, false = not answered.
     * @since 1.0
     */
    public boolean isAnswered()
    {
        return answered;
    }
    /**
     * The mark for this question.
     * 
     * @return The mark, between 0 to 100.
     * @since 1.0
     */
    public double getMark()
    {
        return mark;
    }
    /**
     * Retrieves the instances of criteria belonging to a question.
     * 
     * @param core Current instance of core.
     * @param conn Database connector.
     * @return Array of instances of criteria models; can be empty. This is
     * cached, since it's an expensive operation.
     * @since 1.0
     */
    public InstanceAssignmentCriteria[] getInstanceCriteria(NodeCore core, Connector conn)
    {
        if(cacheCriteria == null)
            cacheCriteria = InstanceAssignmentCriteria.loadAll(core, conn, this);
        return cacheCriteria;
    }

    // Methods - Overrides *****************************************************
    /**
     * Compares an object to the current instance, based on the type and
     * identifier.
     * 
     * @param o The object being compared.
     * @return True = same, false = not the same.
     * @since 1.0
     */
    @Override
    public boolean equals(Object o)
    {
        if(o == null || !(o instanceof InstanceAssignmentQuestion))
            return false;
        InstanceAssignmentQuestion a = (InstanceAssignmentQuestion)o;
        return aiqid == a.aiqid;
    }
    /**
     * The hash-code, based on the instance assignment identifier.
     * 
     * @return The hash-code.
     * @since 1.0
     */
    @Override
    public int hashCode()
    {
        return ia != null ? ia.getAIID() : -1;
    }
}
