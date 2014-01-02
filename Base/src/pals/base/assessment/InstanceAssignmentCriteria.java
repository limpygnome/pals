package pals.base.assessment;

import pals.base.NodeCore;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.database.Result;

/**
 * An instance of an assignment question's criteria.
 */
public class InstanceAssignmentCriteria
{
    // Enums *******************************************************************
    /**
     * The status of the criteria being marked.
     */
    public enum Status
    {
        AwaitingManualMarking(0),
        BeingAnswered(1),
        AwaitingMarking(2),
        Marked(4);
        
        private final int dbValue;
        private Status(int dbValue)
        {
            this.dbValue = dbValue;
        }
        /**
         * Fetches a status value from a db-value.
         * 
         * @param dbValue The database value (integer).
         * @return The status type; set to 'AwaitingManualMarking' if the data
         * cannot be parsed.
         */
        public static Status getStatus(int dbValue)
        {
            switch(dbValue)
            {
                case 1:
                    return BeingAnswered;
                case 2:
                    return AwaitingMarking;
                case 4:
                    return Marked;
                default:
                    return AwaitingManualMarking;
            }
        }
    }
    public enum PersistStatus
    {
        Success,
        Failed,
        Invalid_InstanceAssignmentQuestion,
        Invalid_QuestionCriteria,
        Invalid_Mark
    }
    // Fields ******************************************************************
    private boolean                     persisted;
    private InstanceAssignmentQuestion  iaq;
    private QuestionCriteria            qc;
    private Status                      status;
    private int                         mark;
    // Methods - Constructors **************************************************
    /**
     * Constructs a new model.
     */
    public InstanceAssignmentCriteria()
    {
        this(null, null, Status.AwaitingMarking, 0);
    }
    /**
     * Constructs a new model.
     * 
     * @param iaq The instance of the assignment question; cannot be null.
     * @param qc The question criteria; cannot be null.
     * @param status The status of the criteria.
     * @param mark The mark assigned to the criteria (0 to 100).
     */
    public InstanceAssignmentCriteria(InstanceAssignmentQuestion iaq, QuestionCriteria qc, Status status, int mark)
    {
        this.iaq = iaq;
        this.qc = qc;
        this.status = status;
        this.mark = mark;
    }
    // Methods - Persistence ***************************************************
    /**
     * Loads a model from the database.
     * 
     * @param conn Database connector.
     * @param iaq Instance of the assignment question; cannot be null.
     * @param qc Question criteria; cannot be null.
     * @return An instance of the model or null.
     */
    public static InstanceAssignmentCriteria load(Connector conn, InstanceAssignmentQuestion iaq, QuestionCriteria qc)
    {
        try
        {
            Result res = conn.read("SELECT * FROM pals_assignment_instance_question_criteria WHERE aiqid=? AND qcid=?;", iaq.getAIQID(), qc.getQCID());
            return res.next() ? load(conn, iaq, qc, res) : null;
        }
        catch(DatabaseException ex)
        {
            return null;
        }
    }
    /**
     * Loads a model from the database.
     * 
     * @param core Current instance of the core.
     * @param conn Database connector.
     * @param ia Instance of the assignment; can be null and automatically
     * loaded.
     * @param q The question to which the criteria belongs; can be null and
     * automatically loaded.
     * @param res The result from a query, with next() pre-invoked.
     * @return An instance of the model or null.
     */
    public static InstanceAssignmentCriteria load(NodeCore core, Connector conn, InstanceAssignment ia, Question q, Result res)
    {
        try
        {
            // Load the instance of the question
            InstanceAssignmentQuestion iaq = InstanceAssignmentQuestion.load(core, conn, ia, (int)res.get("aiqid"));
            if(iaq == null)
                return null;
            // Load the question criteria
            QuestionCriteria qc = QuestionCriteria.load(core, conn, q, (int)res.get("qcid"));
            if(qc == null)
                return null;
            // Load model
            return load(conn, iaq, qc, res);
        }
        catch(DatabaseException ex)
        {
            return null;
        }
    }
    /**
     * Loads a model from the database.
     * 
     * @param conn Database connector.
     * @param iaq The instance of the assignment question; cannot be null.
     * @param qc The instance of the question criteria; cannot be null.
     * @param res The result from a query, with next() pre-invoked.
     * @return An instance of the model or null.
     */
    public static InstanceAssignmentCriteria load(Connector conn, InstanceAssignmentQuestion iaq, QuestionCriteria qc, Result res)
    {
        try
        {
            return new InstanceAssignmentCriteria(iaq, qc, Status.getStatus((int)res.get("status")), (int)res.get("mark"));
        }
        catch(DatabaseException ex)
        {
            return null;
        }
    }
    /**
     * Persists the model to the database.
     * 
     * @param conn Database connector.
     * @return The status from the operation.
     */
    public PersistStatus persist(Connector conn)
    {
        // Validate data
        if(iaq == null || !iaq.isPersisted())
            return PersistStatus.Invalid_InstanceAssignmentQuestion;
        else if(qc == null || !qc.isPersisted())
            return PersistStatus.Invalid_QuestionCriteria;
        else if(mark < 0 || mark > 100)
            return PersistStatus.Invalid_Mark;
        else
        {
            try
            {
                // Persist data
                if(persisted)
                {
                    conn.execute("UPDATE pals_assignment_instance_question_criteria SET status=?, mark=? WHERE aiqid=? AND qcid=?;",
                            status.dbValue,
                            mark,
                            iaq.getAIQID(),
                            qc.getQCID()
                            );
                }
                else
                {
                    conn.execute("INSERT INTO pals_assignment_instance_question_criteria (aiqid, qcid, status, mark) VALUES(?,?,?,?);",
                            iaq.getAIQID(),
                            qc.getQCID(),
                            status,
                            mark
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
     * @return True = unpersisted, false = failed.
     */
    public boolean delete(Connector conn)
    {
        try
        {
            conn.execute("DELETE FROM pals_assignment_instance_question_criteria WHERE aiqid=? AND qcid=?;", iaq.getAIQID(), qc.getQCID());
            return true;
        }
        catch(DatabaseException ex)
        {
            return false;
        }
    }
    // Methods - Mutators ******************************************************
    /**
     * @param iaq Sets the instance of the assignment question; this has no
     * effect if the model has been persisted.
     */
    public void setIAQ(InstanceAssignmentQuestion iaq)
    {
        if(!persisted)
            this.iaq = iaq;
    }
    /**
     * @param qc Sets the question criteria; this has no effect if the model
     * has been persisted.
     */
    public void setQC(QuestionCriteria qc)
    {
        if(!persisted)
            this.qc = qc;
    }
    /**
     * @param status Sets the status of this model.
     */
    public void setStatus(Status status)
    {
        this.status = status;
    }
    /**
     * @param mark Sets the mark of this model.
     */
    public void setMark(int mark)
    {
        this.mark = mark;
    }
    // Methods - Accessors *****************************************************
    /**
     * @return Indicates if the model has been persisted.
     */
    public boolean isPersisted()
    {
        return persisted;
    }
    /**
     * @return The instance of the assignment question.
     */
    public InstanceAssignmentQuestion getIAQ()
    {
        return iaq;
    }
    /**
     * @return The question criteria.
     */
    public QuestionCriteria getQC()
    {
        return qc;
    }
    /**
     * @return The status of this model.
     */
    public Status getStatus()
    {
        return status;
    }
    /**
     * @return The mark of this model.
     */
    public int getMark()
    {
        return mark;
    }
}
