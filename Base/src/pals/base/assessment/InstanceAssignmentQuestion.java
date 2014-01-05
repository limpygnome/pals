package pals.base.assessment;

import java.io.IOException;
import pals.base.NodeCore;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.database.Result;
import pals.base.utils.Misc;

/**
 * A model which represents an instance of an assignment question, by a user.
 */
public class InstanceAssignmentQuestion
{
    // Enums *******************************************************************
    public enum PersistStatus
    {
        Success,
        Failed,
        Failed_Serialize,
        Invalid_AssignmentQuestion,
        Invalid_InstanceAssignment
    }
    // Fields ******************************************************************
    private int                 aiqid;              // The assignment-instance question identifier.
    private AssignmentQuestion  aq;                 // The assignment question.
    private InstanceAssignment  ia;                 // The current instance of the assignment.
    private Object              data;               // Data for the current instance of the question.
    // Methods - Constructors **************************************************
    /**
     * Constructs a new instance of an assignment-instance question.
     */
    public InstanceAssignmentQuestion()
    {
        this(null, null, null);
    }
    /**
     * Constructs a new instance of an assignment-instance question.
     * 
     * @param aq The assignment question to instantiate.
     * @param ia The instance of the assignment.
     * @param data The data for the question, provided by a question-type.
     */
    public InstanceAssignmentQuestion(AssignmentQuestion aq, InstanceAssignment ia, Object data)
    {
        this.aiqid = -1;
        this.aq = aq;
        this.ia = ia;
        this.data = data;
    }
    // Methods - Persistence ***************************************************
    /**
     * Loads a persisted model.
     * 
     * @param core Current instance of the core.
     * @param conn Database connector.
     * @param ia The instance of the assignment (if known); can be null to be
     * loaded automatically (possibly expensive).
     * @param aiqid The identifier of the model to load.
     * @return An instance of the model or null.
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
     */
    public static InstanceAssignmentQuestion load(NodeCore core, Connector conn, InstanceAssignment ia, Result res)
    {
        try
        {
            // Deserialize data
            Object data;
            try
            {
                data = Misc.bytesDeserialize((byte[])res.get("data"));
            }
            catch(ClassNotFoundException | IOException ex)
            {
                return null;
            }
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
            InstanceAssignmentQuestion iaq = new InstanceAssignmentQuestion(aq, ia, data);
            iaq.aiqid = (int)res.get("aiqid");
            return iaq;
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
     * @return The status of the operation.
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
                    aiqid = (int)conn.executeScalar("INSERT INTO pals_assignment_instance_question (aqid, aiid, data) VALUES(?,?,?) RETURNING aiqid;",
                            aq.getAQID(),
                            ia.getAIID(),
                            bdata
                            );
                }
                else
                {
                    conn.execute("UPDATE pals_assignment_instance_question SET aqid=?, aiid=?, data=? WHERE aiqid=?;",
                            aq.getAQID(),
                            ia.getAIID(),
                            bdata,
                            aiqid
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
     * @return True = removed, false = failed.
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
            return false;
        }
    }
    // Methods - Mutators ******************************************************
    /**
     * @param aq Sets the assignment-question being instantiated.
     */
    public void setAssignmentQuestion(AssignmentQuestion aq)
    {
        this.aq = aq;
    }
    /**
     * @param ia Sets the assignment instantiation.
     */
    public void setInstanceAssignment(InstanceAssignment ia)
    {
        this.ia = ia;
    }
    /**
     * @param data Sets the data for this instance of the assignment-question.
     */
    public void setData(Object data)
    {
        this.data = data;
    }
    // Methods - Accessors *****************************************************
    /**
     * @return Indicates if the current model is persisted.
     */
    public boolean isPersisted()
    {
        return aiqid != -1;
    }
    /**
     * @return The unique identifier of this model.
     */
    public int getAIQID()
    {
        return aiqid;
    }
    /**
     * @return The assignment-question being instantiated.
     */
    public AssignmentQuestion getAssignmentQuestion()
    {
        return aq;
    }
    /**
     * @return The assignment instantiation.
     */
    public InstanceAssignment getInstanceAssignment()
    {
        return ia;
    }
    /**
     * @return Data for this instance of the assignment-question.
     */
    public Object getData()
    {
        return data;
    }
}
