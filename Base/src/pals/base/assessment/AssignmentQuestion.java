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

import java.util.ArrayList;
import pals.base.Logging;
import pals.base.NodeCore;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.database.Result;

/**
 * A model to represent a question of an assignment (assignment question).
 * 
 * @version 1.0
 */
public class AssignmentQuestion
{
    // Enums *******************************************************************
    /**
     * The status from persisting this model.
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
         * Invalid assignment.
         * 
         * @since 1.0
         */
        Invalid_Assignment,
        /**
         * Invalid question.
         * 
         * @since 1.0
         */
        Invalid_Question,
        /**
         * The specified question has not been properly configured.
         * 
         * @since 1.0
         */
        Invalid_Question_Not_Configured,
        /**
         * Invalid weight.
         * 
         * @since 1.0
         */
        Invalid_Weight,
        /**
         * Invalid page.
         * 
         * @since 1.0
         */
        Invalid_Page,
        /**
         * Invalid page-order.
         * 
         * @since 1.0
         */
        Invalid_PageOrder
    }
    // Fields - Constants ******************************************************
    /**
     * Maximum value for page,
     * 
     * @since 1.0
     */
    public static final int PAGE_LIMIT          = 1000;
    /**
     * Maximum value for page-order.
     * 
     * @since 1.0
     */
    public static final int PAGE_ORDER_LIMIT    = 1000;
    // Fields ******************************************************************
    private int         aqid;
    private Assignment  assignment;
    private Question    question;
    private int         weight;
    private int         page;
    private int         pageOrder;
    // Methods - Constructors **************************************************
    /**
     * Creates a new unpersisted assignment-question.
     * 
     * @since 1.0
     */
    public AssignmentQuestion()
    {
        this(null, null, 0, 1, 1);
    }
    /**
     * Creates a new unpersisted assignment-question.
     * 
     * @param assignment The assignment to which the question belongs.
     * @param question The question.
     * @param weight The weight of the question.
     * @param page The page of the question.
     * @param pageOrder The ordering of the question.
     * @since 1.0
     */
    public AssignmentQuestion(Assignment assignment, Question question, int weight, int page, int pageOrder)
    {
        this.aqid = -1;
        this.assignment = assignment;
        this.question = question;
        this.weight = weight;
        this.page = page;
        this.pageOrder = pageOrder;
    }
    // Methods - Persistence ***************************************************
    /**
     * Loads all of the models associated with an assignment.
     * 
     * @param core Current instance of the core.
     * @param conn Database connector.
     * @param ass The assignment to which the models belong.
     * @return Array of models; can be empty.
     * @since 1.0
     */
    public static AssignmentQuestion[] loadAll(NodeCore core, Connector conn, Assignment ass)
    {
        try
        {
            ArrayList<AssignmentQuestion> buffer = new ArrayList<>();
            Result res = conn.read("SELECT * FROM pals_assignment_questions WHERE assid=? ORDER BY page ASC, page_order ASC;", ass.getAssID());
            AssignmentQuestion aq;
            while(res.next())
            {
                if((aq = load(core, conn, ass, res)) != null)
                    buffer.add(aq);
            }
            return buffer.toArray(new AssignmentQuestion[buffer.size()]);
        }
        catch(DatabaseException ex)
        {
            core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return new AssignmentQuestion[0];
        }
    }
    /**
     * Loads a persisted assignment question.
     * 
     * @param core Current instance of the core.
     * @param conn Database connector.
     * @param ass Assignment; if null, this is loaded - possibly quite
     * expensive.
     * @param aqid The identifier of the assignment question.
     * @return An instance of the model or null.
     * @since 1.0
     */
    public static AssignmentQuestion load(NodeCore core, Connector conn, Assignment ass, int aqid)
    {
        try
        {
            Result res;
            if(ass == null)
                res = conn.read("SELECT * FROM pals_assignment_questions WHERE aqid=?;", aqid);
            else
                res = conn.read("SELECT * FROM pals_assignment_questions WHERE aqid=? AND assid=?;", aqid, ass.getAssID()); 
            return res.next() ? load(core, conn, ass, res) : null;
        }
        catch(DatabaseException ex)
        {
            core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return null;
        }
    }
    /**
     * Loads a persisted assignment question.
     * 
     * @param core Current instance of the core.
     * @param conn Database connector.
     * @param ass Assignment; if null, this is loaded - possibly quite
     * expensive.
     * @param res The result of the query; this should have next() pre-invoked.
     * @return An instance of the model or null.
     * @since 1.0
     */
    public static AssignmentQuestion load(NodeCore core, Connector conn, Assignment ass, Result res)
    {
        try
        {
            // Load the assignment, if null
            if(ass == null)
            {
                ass = Assignment.load(conn, null, (int)res.get("assid"));
            }
            // Load the question
            Question question = Question.load(core, conn, (int)res.get("qid"));
            // Create and return instance
            AssignmentQuestion aq = new AssignmentQuestion(ass, question, (int)res.get("weight"), (int)res.get("page"), (int)res.get("page_order"));
            aq.aqid = (int)res.get("aqid");
            return aq;
        }
        catch(DatabaseException ex)
        {
            core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return null;
        }
    }
    /**
     * Persists the model to the database; if the assignment-question has not
     * been persisted, it is assigned an identifier.
     * 
     * @param conn Database connector.
     * @return The status of the operation.
     * @since 1.0
     */
    public PersistStatus persist(Connector conn)
    {
        // Validate data
        if(assignment == null || !assignment.isPersisted())
            return PersistStatus.Invalid_Assignment;
        else if(question == null || !question.isPersisted())
            return PersistStatus.Invalid_Question;
        else if(weight <= 0)
            return PersistStatus.Invalid_Weight;
        else if(page < 1 || page > PAGE_LIMIT)
            return PersistStatus.Invalid_Page;
        else if(pageOrder < 1 || pageOrder > PAGE_ORDER_LIMIT)
            return PersistStatus.Invalid_PageOrder;
        else
        {
            // Persist data
            try
            {
                // Check the question has been configured
                if((long)conn.executeScalar("SELECT COUNT('') FROM pals_question_criteria WHERE qid=?;", question.getQID()) == 0 || (int)conn.executeScalar("SELECT CAST((data IS NOT NULL) AS int) FROM pals_question WHERE qid=?;", question.getQID()) == 0)
                    return PersistStatus.Invalid_Question_Not_Configured;
                else if(aqid == -1)
                {
                    aqid = (int)conn.executeScalar("INSERT INTO pals_assignment_questions (assid, qid, weight, page, page_order) VALUES(?,?,?,?,?) RETURNING aqid;",
                            assignment.getAssID(),
                            question.getQID(),
                            weight,
                            page,
                            pageOrder
                            );
                }
                else
                {
                    conn.execute("UPDATE pals_assignment_questions SET assid=?, qid=?, weight=?, page=?, page_order=? WHERE aqid=?;",
                            assignment.getAssID(),
                            question.getQID(),
                            weight,
                            page,
                            pageOrder,
                            aqid
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
     * @return True = deleted, false = failed.
     * @since 1.0
     */
    public boolean delete(Connector conn)
    {
        if(aqid == -1)
            return false;
        try
        {
            conn.execute("DELETE FROM pals_assignment_questions WHERE aqid=?;", aqid);
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
     * Sets the assignment to which the question belongs.
     * 
     * @param assignment The assignment to which this question belongs.
     * @since 1.0
     */
    public void setAssignment(Assignment assignment)
    {
        this.assignment = assignment;
    }
    /**
     * Sets the question of this assignment question.
     * 
     * @param question The question used.
     * @since 1.0
     */
    public void setQuestion(Question question)
    {
        this.question = question;
    }
    /**
     * Sets the weight of the assignment question, relative to the
     * assignment.
     * 
     * @param weight The weight of the question; can be any arbitrary
     * positive number.
     * @since 1.0
     */
    public void setWeight(int weight)
    {
        this.weight = weight;
    }
    /**
     * Sets the page of the question.
     * 
     * @param page The page of the question; must be greater than zero. Refer
     * to {@link #PAGE_LIMIT} for the limit.
     * @since 1.0
     */
    public void setPage(int page)
    {
        this.page = page;
    }
    /**
     * Sets the order of which the question appears on a page.
     * 
     * @param pageOrder The order of which the question is displayed on the
     * page. Refer to {@link #PAGE_ORDER_LIMIT} for the limit.
     * @since 1.0
     */
    public void setPageOrder(int pageOrder)
    {
        this.pageOrder = pageOrder;
    }
    // Methods - Accessors *****************************************************
    /**
     * Indicates if the model has been persisted.
     * 
     * @return Indicates if the model has been persisted.
     * @since 1.0
     */
    public boolean isPersisted()
    {
        return aqid != -1;
    }
    /**
     * The identifier of this assignment question.
     * 
     * @return The identifier of the assignment question; allows for multiple
     * instances of questions for a single assignment.
     * @since 1.0
     */
    public int getAQID()
    {
        return aqid;
    }
    /**
     * The assignment to which this question belongs.
     * 
     * @return The assignment to which this question belongs.
     * @since 1.0
     */
    public Assignment getAssignment()
    {
        return assignment;
    }
    /**
     * The question used for this assignment question.
     * 
     * @return The question.
     * @since 1.0
     */
    public Question getQuestion()
    {
        return question;
    }
    /**
     * The weight of the assignment question, relative to the assignment.
     * 
     * @return The weight of the assignment question.
     * @since 1.0
     */
    public int getWeight()
    {
        return weight;
    }
    /**
     * The page of which the assignment question appears.
     * 
     * @return The page.
     * @since 1.0
     */
    public int getPage()
    {
        return page;
    }
    /**
     * The order/priority of the question on a page.
     * 
     * @return The order of which the question is displayed on the page.
     * @since 1.0
     */
    public int getPageOrder()
    {
        return pageOrder;
    }
}
