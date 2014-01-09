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
        
        public final int dbValue;
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
     * Creates models for the answered questions of an instance of an
     * assignment. This is executed as a transaction. Old criterias are not
     * modified where the status is AwaitingManualMarking or Marked.
     * 
     * @param conn Database connector.
     * @param ia Instance assignment.
     * @param status The status to give each model.
     * @return True = successful, false = failed (status of operation).
     */
    public static boolean createForInstanceAssignment(Connector conn, InstanceAssignment ia, Status status)
    {
        try
        {
            conn.execute("BEGIN;");
            conn.execute("DELETE FROM pals_assignment_instance_question_criteria WHERE NOT (status=? OR status=?) AND aiqid IN (SELECT aiqid FROM pals_assignment_instance_question WHERE aiid=?);",
                    Status.AwaitingManualMarking.dbValue,
                    Status.Marked.dbValue,
                    ia.getAIID()
            );
            // Fetch the required information
            Result res = conn.read("SELECT aiq.aiqid, qc.qcid FROM pals_question_criteria AS qc "
                    + "LEFT OUTER JOIN pals_assignment_questions AS aq ON qc.qid=aq.qid "
                    + "LEFT OUTER JOIN pals_assignment_instance AS ai ON aq.assid=ai.assid "
                    + "LEFT OUTER JOIN pals_assignment_instance_question AS aiq ON (aiq.aiid=ai.aiid AND aiq.aqid=aq.aqid) "
                    + "LEFT OUTER JOIN pals_assignment_instance_question_criteria AS aiqc ON (aiqc.qcid=qc.qcid AND aiqc.aiqid=aiq.aiqid) "
                    + "WHERE ai.aiid=? AND aiq.answered='1' AND (aiqc.status IS NULL OR NOT(aiqc.status=? OR aiqc.status=?));",
                    ia.getAIID(),
                    Status.AwaitingManualMarking.dbValue,
                    Status.Marked.dbValue
            );
            // Create a model for each criteria
            while(res.next())
            {
                conn.execute("INSERT INTO pals_assignment_instance_question_criteria (aiqid,qcid,status,mark) VALUES(?,?,?,?);", (int)res.get("aiqid"), (int)res.get("qcid"), status.dbValue, 0);
            }
            conn.execute("COMMIT;");
            return true;
        }
        catch(DatabaseException ex)
        {
            try
            {
                conn.execute("ROLLBACK;");
            }
            catch(DatabaseException ex2)
            {
            }
            return false;
        }
    }
    /**
     * Fetches the model of the next criteria for processing/marking;
     * this will update the last_processed column.
     * 
     * @param core Current instance of the core.
     * @param conn Database connector.
     * @param timeoutMs The timeout period for the last_processed column.
     * @return An instance of the model or null.
     */
    public static InstanceAssignmentCriteria loadNextWork(NodeCore core, Connector conn, int timeoutMs)
    {
        try
        {
            Result res = conn.read("SELECT * FROM pals_assignment_instance_question_criteria WHERE status=? AND (last_processed IS NULL OR last_processed < (current_timestamp - CAST(? AS INTERVAL))) LIMIT 1;", Status.AwaitingMarking.dbValue, timeoutMs+" millisecond");
            if(res.next())
            {
                conn.execute("UPDATE pals_assignment_instance_question_criteria SET last_processed=current_timestamp WHERE aiqid=? AND qcid=?;", (int)res.get("aiqid"), (int)res.get("qcid"));
                return load(core, conn, null, null, res);
            }
            else
                return null;
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
            InstanceAssignmentCriteria iac = new InstanceAssignmentCriteria(iaq, qc, Status.getStatus((int)res.get("status")), (int)res.get("mark"));
            iac.persisted = true;
            return iac;
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
                            status.dbValue,
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
