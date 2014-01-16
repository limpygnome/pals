package pals.base.assessment;

import java.io.IOException;
import java.util.ArrayList;
import pals.base.NodeCore;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.database.Result;
import pals.base.utils.Misc;

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
        /**
         * The criteria is awaiting manual-marking from a human.
         */
        AwaitingManualMarking(0),
        /**
         * The criteria is being answered by a user; this means the criteria
         * is active and should not be marked.
         */
        BeingAnswered(1),
        /**
         * The criteria is queued for marking.
         */
        AwaitingMarking(2),
        /**
         * The criteria has been marked.
         */
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
        Failed_Serialize,
        Invalid_InstanceAssignmentQuestion,
        Invalid_QuestionCriteria,
        Invalid_Mark
    }
    /**
     * Indicates the status of creating criteria for an instance of an
     * assignment.
     */
    public enum CreateInstanceStatus
    {
        /**
         * Operation failed due to an error.
         */
        Failed,
        /**
         * No instance question data exists, therefore no criteria were
         * created.
         */
        Failed_NoInstanceQuestions,
        /**
         * Successfully created.
         */
        Success
    }
    // Fields ******************************************************************
    private boolean                     persisted;
    private InstanceAssignmentQuestion  iaq;
    private QuestionCriteria            qc;
    private Status                      status;
    private int                         mark;
    private Object                      data;
    // Methods - Constructors **************************************************
    /**
     * Constructs a new model.
     */
    public InstanceAssignmentCriteria()
    {
        this(null, null, Status.AwaitingMarking, 0, null);
    }
    /**
     * Constructs a new model.
     * 
     * @param iaq The instance of the assignment question; cannot be null.
     * @param qc The question criteria; cannot be null.
     * @param status The status of the criteria.
     * @param mark The mark assigned to the criteria (0 to 100).
     * @param data Serialized data; can be null.
     */
    public InstanceAssignmentCriteria(InstanceAssignmentQuestion iaq, QuestionCriteria qc, Status status, int mark, Object data)
    {
        this.iaq = iaq;
        this.qc = qc;
        this.status = status;
        this.mark = mark;
        this.data = data;
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
     * @return The status from creating instances of criteria for the instances
     * of questions for the instance of an assignment.
     */
    public static CreateInstanceStatus createForInstanceAssignment(Connector conn, InstanceAssignment ia, Status status)
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
            boolean hasRows = res.next();
            if(hasRows)
            {
                do
                {
                    conn.execute("INSERT INTO pals_assignment_instance_question_criteria (aiqid,qcid,status,mark) VALUES(?,?,?,?);", (int)res.get("aiqid"), (int)res.get("qcid"), status.dbValue, 0);
                }
                while(res.next());
            }
            conn.execute("COMMIT;");
            return hasRows ? CreateInstanceStatus.Success : CreateInstanceStatus.Failed_NoInstanceQuestions;
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
            return CreateInstanceStatus.Failed;
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
                return loadAuto(core, conn, null, null, res);
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
     * @param core Current instance of the core.
     * @param conn Database connector.
     * @param iaq Instance of the assignment question; cannot be null.
     * @param qc Question criteria; cannot be null.
     * @return An instance of the model or null.
     */
    public static InstanceAssignmentCriteria load(NodeCore core, Connector conn, InstanceAssignmentQuestion iaq, QuestionCriteria qc)
    {
        try
        {
            Result res = conn.read("SELECT * FROM pals_assignment_instance_question_criteria WHERE aiqid=? AND qcid=?;", iaq.getAIQID(), qc.getQCID());
            return res.next() ? load(core, conn, iaq, qc, res) : null;
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
    public static InstanceAssignmentCriteria loadAuto(NodeCore core, Connector conn, InstanceAssignment ia, Question q, Result res)
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
            return load(core, conn, iaq, qc, res);
        }
        catch(DatabaseException ex)
        {
            return null;
        }
    }
    /**
     * Loads a model from the database.
     * 
     * @param core The current instance of the core.
     * @param conn Database connector.
     * @param iaq The instance of the assignment question; cannot be null.
     * @param qc The instance of the question criteria; cannot be null.
     * @param res The result from a query, with next() pre-invoked.
     * @return An instance of the model or null.
     */
    public static InstanceAssignmentCriteria load(NodeCore core, Connector conn, InstanceAssignmentQuestion iaq, QuestionCriteria qc, Result res)
    {
        try
        {
            Object data;
            if(res.get("cdata") != null)
            {
                try
                {
                    data = Misc.bytesDeserialize(core, (byte[])res.get("cdata"));
                }
                catch(ClassNotFoundException | IOException ex)
                {
                    return null;
                }
            }
            else
                data = null;
            InstanceAssignmentCriteria iac = new InstanceAssignmentCriteria(iaq, qc, Status.getStatus((int)res.get("status")), (int)res.get("mark"), data);
            iac.persisted = true;
            return iac;
        }
        catch(DatabaseException ex)
        {
            return null;
        }
    }
    /**
     * Loads all of the models for an instance of a question.
     * 
     * @param core Current instance of core.
     * @param conn Database connector.
     * @param iaq An instance of an assignment question.
     * @return Array of criterias; can be empty.
     */
    public static InstanceAssignmentCriteria[] loadAll(NodeCore core, Connector conn, InstanceAssignmentQuestion iaq)
    {
        try
        {
            ArrayList<InstanceAssignmentCriteria> buffer = new ArrayList<>();
            Result data = conn.read("SELECT * FROM pals_assignment_instance_question_criteria AS aiqc, pals_question_criteria AS qc WHERE aiqc.aiqid=? AND qc.qcid=aiqc.qcid;", iaq.getAIQID());
            InstanceAssignmentCriteria iac;
            QuestionCriteria qc;
            while(data.next())
            {
                if((qc = QuestionCriteria.load(core, conn, iaq.getAssignmentQuestion().getQuestion(), data)) != null && (iac = load(core, conn, iaq, qc)) != null)
                    buffer.add(iac);
            }
            return buffer.toArray(new InstanceAssignmentCriteria[buffer.size()]);
        }
        catch(DatabaseException ex)
        {
            return new InstanceAssignmentCriteria[0];
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
                byte[] serializedData = Misc.bytesSerialize(data);
                // Persist data
                if(persisted)
                {
                    conn.execute("UPDATE pals_assignment_instance_question_criteria SET status=?, mark=?, cdata=? WHERE aiqid=? AND qcid=?;",
                            status.dbValue,
                            mark,
                            serializedData,
                            iaq.getAIQID(),
                            qc.getQCID()
                            );
                }
                else
                {
                    conn.execute("INSERT INTO pals_assignment_instance_question_criteria (aiqid, qcid, status, mark, cdata) VALUES(?,?,?,?,?);",
                            iaq.getAIQID(),
                            qc.getQCID(),
                            status.dbValue,
                            mark,
                            serializedData
                            );
                }
                return PersistStatus.Success;
            }
            catch(IOException ex)
            {
                return PersistStatus.Failed_Serialize;
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
    /**
     * Deletes all of the instances of criteria belonging to an assignment.
     * 
     * @param conn Database connector.
     * @param ia The instance of an assignment.
     * @return True = success, false = failed.
     */
    public static boolean delete(Connector conn, InstanceAssignment ia)
    {
        try
        {
            conn.execute("DELETE FROM pals_assignment_instance_question_criteria AS aiqc WHERE aiqc.aiqid IN (SELECT aiqid FROM pals_assignment_instance_question WHERE aiid=?);", ia.getAIID());
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
    /**
     * @param data The unique data to be set for this instance; can be null.
     */
    public void setData(Object data)
    {
        this.data = data;
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
    /**
     * @return The unique data for this instance of the criteria.
     */
    public Object getData()
    {
        return data;
    }
}
