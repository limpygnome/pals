package pals.base.assessment;

import java.io.IOException;
import java.io.Serializable;
import pals.base.UUID;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.database.Result;
import pals.base.utils.Misc;

/**
 * A criteria for marking a question.
 */
public class QuestionCriteria
{
    // Enums *******************************************************************
    public enum PersistStatus
    {
        Success,
        Failed,
        Failed_Serialize,
        Invalid_Question,
        Invalid_Criteria,
        Invalid_Weight
    }
    // Fields ******************************************************************
    private int             qcid;       // Unique identifier for this model.
    private Question        question;   // The question to which this, criteria, belongs.
    private TypeCriteria    criteria;   // The type of criteria.
    private Object          data;       // Any data stored by the criteria-type.
    private int             weight;     // Weight of criteria.
    // Methods - Constructors **************************************************
    /**
     * Creates a new unpersisted model.
     */
    public QuestionCriteria()
    {
        this(null, null, null, 0);
    }
    /**
     * Creates a new unpersisted model.
     * 
     * @param question The question to which this, criteria, belongs.
     * @param criteria The criteria-type.
     * @param data Any data for the criteria-type.
     * @param weight The weight of the criteria.
     */
    public QuestionCriteria(Question question, TypeCriteria criteria, Object data, int weight)
    {
        this.qcid = -1;
        this.question = question;
        this.criteria = criteria;
        this.data = data;
        this.weight = weight;
    }
    // Methods - Persistence ***************************************************
    /**
     * Loads a persisted model.
     * 
     * @param conn Database connector.
     * @param question The question to which this criteria belongs; if this is
     * null, the question model is loaded (could be expensive for multiple
     * criterias).
     * @param qcid The identifier of the criteria.
     * @return An instance of the model or null.
     */
    public static QuestionCriteria load(Connector conn, Question question, int qcid)
    {
        try
        {
            Result res = conn.read("SELECT * FROM pals_question_criteria WHERE qcid=?;", qcid);
            return res.next() ? load(conn, question, res) : null;
        }
        catch(DatabaseException ex)
        {
            return null;
        }
    }
    /**
     * Loads a persisted model.
     * 
     * @param conn Database connector.
     * @param question The question to which this criteria belongs; if this is
     * null, the question model is loaded (could be expensive for multiple
     * criterias).
     * @param result The result of a query, with the method next() pre-invoked.
     * @return An instance of the model or null.
     */
    public static QuestionCriteria load(Connector conn, Question question, Result result)
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
                question = Question.load(conn, (int)result.get("qid"));
            }
            // Deserialize data
            Object data;
            try
            {
                data = Misc.bytesDeserialize((byte[])result.get("data"));
            }
            catch(IOException | ClassNotFoundException ex)
            {
                return null;
            }
            // Create instance and return
            QuestionCriteria qc = new QuestionCriteria(question, tc, data, (int)result.get("weight"));
            qc.qcid = (int)result.get("qcid");
            return qc;
        }
        catch(DatabaseException ex)
        {
            return null;
        }
    }
    /**
     * Persists the model; if the model is unpersisted, it's assigned a new
     * identifier (qcid).
     * 
     * @param conn Database connector.
     * @return The status of the operation.
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
                    qcid = (int)conn.executeScalar("INSERT INTO pals_question_criteria (qid, uuid_ctype, data, weight) VALUES(?,?,?,?) RETURNING qcid;",
                            question.getQID(),
                            criteria.getUuidCType().getBytes(),
                            bdata,
                            weight
                            );
                }
                else
                {
                    conn.execute("UPDATE pals_question_criteria SET qid=?, uuid_ctype=?, data=?, weight=? WHERE qcid=?;",
                            question.getQID(),
                            criteria.getUuidCType().getBytes(),
                            bdata,
                            weight,
                            qcid
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
     * Unpersists the criteria.
     * 
     * @param conn Database connector.
     * @return True = removed, false = failed.
     */
    public boolean remove(Connector conn)
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
            return false;
        }
    }
    // Methods - Mutators ******************************************************
    /**
     * @param question The question to which this criteria belongs.
     */
    public void setQuestion(Question question)
    {
        this.question = question;
    }
    /**
     * @param criteria The type of criteria.
     */
    public void setCriteria(TypeCriteria criteria)
    {
        this.criteria = criteria;
    }
    /**
     * @param data Any data, which can be serialized, set by the criteria-type.
     * @param <T> Serializable data-type.
     */
    public <T extends Serializable> void  setData(T data)
    {
        this.data = data;
    }
    /**
     * @param weight The weight of this criteria.
     */
    public void setWeight(int weight)
    {
        this.weight = weight;
    }
    // Methods - Accessors *****************************************************
    /**
     * @return Indicates if the model has been persisted.
     */
    public boolean isPersisted()
    {
        return qcid != -1;
    }
    /**
     * @return The identifier of this criteria.
     */
    public int getQCID()
    {
        return qcid;
    }
    /**
     * @return The question to which this, criteria, belongs.
     */
    public Question getQuestion()
    {
        return question;
    }
    /**
     * @return The type of criteria.
     */
    public TypeCriteria getCriteria()
    {
        return criteria;
    }
    /**
     * @return The custom data set by the criteria-type.
     */
    public Object getData()
    {
        return data;
    }
    /**
     * @return The weight of the criteria.
     */
    public int getWeight()
    {
        return weight;
    }
}
