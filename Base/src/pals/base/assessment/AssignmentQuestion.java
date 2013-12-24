package pals.base.assessment;

import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.database.Result;

/**
 * A model to represent a question of an assignment (assignment question).
 */
public class AssignmentQuestion
{
    // Enums *******************************************************************
    public enum PersistStatus
    {
        Success,
        Failed,
        Invalid_Assignment,
        Invalid_Question,
        Invalid_Weight
    }
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
     * Loads a persisted assignment question.
     * 
     * @param conn Database connector.
     * @param ass Assignment; if null, this is loaded - possibly quite
     * expensive.
     * @param aqid The identifier of the assignment question.
     * @return An instance of the model or null.
     */
    public static AssignmentQuestion load(Connector conn, Assignment ass, int aqid)
    {
        try
        {
            Result res = conn.read("SELECT * FROM pals_assignment_questions WHERE aqid=?;", aqid);
            return res.next() ? load(conn, ass, res) : null;
        }
        catch(DatabaseException ex)
        {
            return null;
        }
    }
    /**
     * Loads a persisted assignment question.
     * 
     * @param conn Database connector.
     * @param ass Assignment; if null, this is loaded - possibly quite
     * expensive.
     * @param res The result of the query; this should have next() pre-invoked.
     * @return An instance of the model or null.
     */
    public static AssignmentQuestion load(Connector conn, Assignment ass, Result res)
    {
        try
        {
            // Load the assignment, if null
            if(ass == null)
            {
                ass = Assignment.load(conn, null, (int)res.get("assid"));
            }
            // Load the question
            Question question = Question.load(conn, (int)res.get("qid"));
            // Create and return instance
            AssignmentQuestion aq = new AssignmentQuestion(ass, question, (int)res.get("weight"), (int)res.get("page"), (int)res.get("page_order"));
            aq.aqid = (int)res.get("aqid");
            return aq;
        }
        catch(DatabaseException ex)
        {
            return null;
        }
    }
    /**
     * Persists the model to the database; if the assignment-question has not
     * been persisted, it is assigned an identifier.
     * 
     * @param conn Database connector.
     * @return The status of the operation.
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
        else
        {
            // Persist data
            try
            {
                if(aqid == -1)
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
                return PersistStatus.Failed;
            }
        }
    }
    /**
     * Unpersists the model from the database.
     * 
     * @param conn Database connector.
     * @return True = deleted, false = failed.
     */
    public boolean delete(Connector conn)
    {
        if(aqid == -1)
            return false;
        try
        {
            conn.execute("DELETE FROM pals_assignment_questions WHERE sqid=?;", aqid);
            return true;
        }
        catch(DatabaseException ex)
        {
            return false;
        }
    }
    // Methods - Mutators ******************************************************
    /**
     * @param assignment The assignment to which this question belongs.
     */
    public void setAssignment(Assignment assignment)
    {
        this.assignment = assignment;
    }
    /**
     * @param question The question used.
     */
    public void setQuestion(Question question)
    {
        this.question = question;
    }
    /**
     * @param weight The weight of the question.
     */
    public void setWeight(int weight)
    {
        this.weight = weight;
    }
    /**
     * @param page The page of the question; must be greater than zero.
     */
    public void setPage(int page)
    {
        this.page = page;
    }
    /**
     * @param pageOrder The order of which the question is displayed on the
     * page.
     */
    public void setPageOrder(int pageOrder)
    {
        this.pageOrder = pageOrder;
    }
    // Methods - Accessors *****************************************************
    /**
     * @return Indicates if the model has been persisted.
     */
    public boolean isPersisted()
    {
        return aqid == -1;
    }
    /**
     * @return The identifier of the assignment question; allows for multiple
     * instances of questions for a single assignment.
     */
    public int getAqID()
    {
        return aqid;
    }
    /**
     * @return The assignment to which this question belongs.
     */
    public Assignment getAssignment()
    {
        return assignment;
    }
    /**
     * @return The question used.
     */
    public Question getQuestion()
    {
        return question;
    }
    /**
     * @return The weight of the assignment question.
     */
    public int getWeight()
    {
        return weight;
    }
    /**
     * @return The page of which to display the question on.
     */
    public int getPage()
    {
        return page;
    }
    /**
     * @return The order of which the question is displayed on the page.
     */
    public int getPageOrder()
    {
        return pageOrder;
    }
}
