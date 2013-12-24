package pals.base.assessment;

import java.io.IOException;
import java.io.Serializable;
import pals.base.UUID;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.database.Result;
import pals.base.utils.Misc;

/**
 * A model which represents a question, of an assignment.
 */
public class Question
{
    // Enums *******************************************************************
    public enum PersistStatus
    {
        Failed,
        Failed_Serialize,
        Success,
        Invalid_QuestionType,
        Invalid_Title
    }
    // Fields ******************************************************************
    private int             qid;
    private TypeQuestion    qtype;
    private String          title;
    private Object          data;
    // Methods - Constructors **************************************************
    /**
     * Creates a new nullified unpersisted model.
     */
    public Question()
    {
        this(null, null, null);
    }
    /**
     * Creates a new unpersisted model.
     * 
     * @param qtype Type of question.
     * @param title Title of the question.
     * @param data The question's data.
     */
    public Question(TypeQuestion qtype, String title, Object data)
    {
        this.qid = -1;
        this.qtype = qtype;
        this.title = title;
        this.data = data;
    }
    // Methods - Persistence ***************************************************
    /**
     * Loads a persisted model by its identifier.
     * 
     * @param conn Database connector.
     * @param qid Question identifier.
     * @return Instance of model or null.
     */
    public static Question load(Connector conn, int qid)
    {
        try
        {
            Result res = conn.read("SELECT * FROM pals_question WHERE qid=?;", qid);
            return res.next() ? load(conn, res) : null;
        }
        catch(DatabaseException ex)
        {
            return null;
        }
    }
    /**
     * Loads a persisted model from a result; next() should be pre-invoked.
     * 
     * @param conn Database connector.
     * @param result Database result.
     * @return Instance of model or null.
     */
    public static Question load(Connector conn, Result result)
    {
        try
        {
            // Fetch qtype
            TypeQuestion tq = TypeQuestion.load(conn, UUID.parse((byte[])result.get("uuid_qtype")));
            if(tq == null)
                return null;
            // Read serialized object
            Object obj;
            try
            {
                obj = Misc.bytesDeserialize((byte[])result.get("data"));
            }
            catch(IOException | ClassNotFoundException ex)
            {
                return null;
            }
            // Create and return instance
            Question q = new Question(tq, (String)result.get("title"), obj);
            q.qid = (int)result.get("qid");
            return q;
        }
        catch(DatabaseException ex)
        {
            return null;
        }
    }
    /**
     * Persists the model; for unpersisted models, the qid is updated with the
     * newly assigned identifier.
     * 
     * @param conn Database connector.
     * @return The status of the operation.
     */
    public PersistStatus persist(Connector conn)
    {
        try
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
            // Persist
            if(qid == -1)
            {
                qid = (int)conn.executeScalar("INSERT INTO pals_question (uuid_qtype, title, data) VALUES(?,?,?) RETURNING qid;", qtype.getUuidQType().getBytes(), title, bdata);
            }
            else   
            {
                conn.execute("UPDATE pals_question SET uuid_qtype=?, title=?, data=? WHERE qid=?;", qtype.getUuidQType().getBytes(), title, bdata, qid);
            }
            return PersistStatus.Success;
        }
        catch(DatabaseException ex)
        {
            return PersistStatus.Failed;
        }
    }
    /**
     * Removes the model from the database.
     * 
     * @param conn Database connector.
     * @return True = removed, false = failed.
     */
    public boolean remove(Connector conn)
    {
        try
        {
            conn.execute("DELETE FROM pals_question WHERE qid=?;", qid);
            return true;
        }
        catch(DatabaseException ex)
        {
            return false;
        }
    }
    // Methods - Mutators ******************************************************
    /**
     * @param qtype The type of question.
     */
    public void setQtype(TypeQuestion qtype)
    {
        this.qtype = qtype;
    }
    /**
     * @param title The title of the question.
     */
    public void setTitle(String title)
    {
        this.title = title;
    }
    /**
     * @param data Serializable data, which can be used by a question-type to
     * render/handle the question.
     * @param <T> The type of data must be serializable.
     */
    public <T extends Serializable> void setData(T data)
    {
        this.data = data;
    }
    // Methods - Accessors *****************************************************
    /**
     * @return Indicates if the model has been persisted.
     */
    public boolean isPersisted()
    {
        return qid == -1;
    }
    /**
     * @return The identifier of this question.
     */
    public int getQID()
    {
        return qid;
    }
    /**
     * @return The type of question.
     */
    public TypeQuestion getQtype()
    {
        return qtype;
    }
    /**
     * @return The question's title.
     */
    public String getTitle()
    {
        return title;
    }
    /**
     * @return Data used by the question-type.
     * @param <T> The data-type of the object is serializable. 
     */
    public <T extends Serializable> T getData()
    {
        return (T)data;
    }
}
