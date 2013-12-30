package pals.base.assessment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import pals.base.UUID;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.database.Result;

/**
 * A model to represent a type of question.
 */
public class TypeQuestion
{
    // Enums *******************************************************************
    public enum PersistStatus
    {
        Success,
        Failed,
        Invalid_UUID,
        Invalid_PluginUUID,
        Invalid_Title,
        Invalid_Description,
    }
    public enum CriteriaPersistStatus
    {
        Success,
        Failed
    }
    // Fields ******************************************************************
    private boolean                     persisted;      // Indicates of the model has been persisted.
    private UUID                        uuidQType;      // The UUID of this type.
    private UUID                        uuidPlugin;     // The plugin which owns/handles the type.
    private String                      title;          // The title of the type.
    private String                      description;    // A description of the type. 
    private HashMap<UUID,TypeCriteria>  criterias;   // The available criterias for the type.
    // Methods - Constructors **************************************************
    public TypeQuestion()
    {
        this(null, null, null, null);
    }
    public TypeQuestion(UUID uuidQType, UUID uuidPlugin, String title, String description)
    {
        this.persisted = false;
        this.uuidQType = uuidQType;
        this.uuidPlugin = uuidPlugin;
        this.title = title;
        this.description = description;
        this.criterias = new HashMap<>();
    }
    // Methods - Persistence ***************************************************
    /**
     * Loads all the persisted question-types.
     * 
     * @param conn Database connector.
     * @return Array of types of questions available.
     */
    public static TypeQuestion[] loadAll(Connector conn)
    {
        try
        {
            Result res = conn.read("SELECT * FROM pals_question_types;");
            TypeQuestion q;
            ArrayList<TypeQuestion> buffer = new ArrayList<>();
            while(res.next())
            {
                if((q = load(res)) != null)
                    buffer.add(q);
            }
            return buffer.toArray(new TypeQuestion[buffer.size()]);
        }
        catch(DatabaseException ex)
        {
            return new TypeQuestion[0];
        }
    }
    /**
     * Loads a persisted model from the database.
     * 
     * @param conn Database connector.
     * @param uuidQType The UUID of the model.
     * @return An instance of the model or null.
     */
    public static TypeQuestion load(Connector conn, UUID uuidQType)
    {
        if(uuidQType == null)
            return null;
        try
        {
            Result res = conn.read("SELECT * FROM pals_question_types WHERE uuid_qtype=?;", uuidQType.getBytes());
            return res.next() ? load(res) : null;
        }
        catch(DatabaseException ex)
        {
            return null;
        }
    }
    /**
     * Loads a persisted model from a result.
     * 
     * @param result The result with the data; next() should be pe-invoked.
     * @return An instance of the model or null.
     */
    public static TypeQuestion load(Result result)
    {
        try
        {
            TypeQuestion tq = new TypeQuestion(UUID.parse((byte[])result.get("uuid_qtype")), UUID.parse((byte[])result.get("uuid_plugin")), (String)result.get("title"), (String)result.get("description"));
            tq.persisted = true;
            return tq;
        }
        catch(DatabaseException ex)
        {
            return null;
        }
    }
    /**
     * Loads the criteria-types available for this type of question; by default,
     * these are not loaded.
     * 
     * @param conn Database connector.
     * @return True = loaded, false = failed.
     */
    public boolean loadCriterias(Connector conn)
    {
        try
        {
            criterias.clear();
            Result res = conn.read("SELECT * FROM pals_qtype_ctype WHERE uuid_qtype=?;", uuidQType.getBytes());
            TypeCriteria tc;
            while(res.next())
            {
                if((tc = TypeCriteria.load(res)) != null)
                    criterias.put(tc.getUuidCType(), tc);
            }
            return true;
        }
        catch(DatabaseException ex)
        {
            return false;
        }
    }
    /**
     * Persists the model.
     * 
     * @param conn Database connector.
     * @return Status from the operation.
     */
    public PersistStatus persist(Connector conn)
    {
        // Validate data
        if(uuidQType == null)
            return PersistStatus.Invalid_UUID;
        else if(uuidPlugin == null)
            return PersistStatus.Invalid_PluginUUID;
        else if(title == null)
            return PersistStatus.Invalid_Title;
        else if(description == null || description.length() < 0)
            return PersistStatus.Invalid_Description;
        else
        {
            // Attempt to persist data
            try
            {
                if(persisted)
                {
                    conn.execute("UPDATE pals_question_types SET uuid_plugin=?, title=?, description=? WHERE uuid_qtype=?;", uuidPlugin.getBytes(), title, description, uuidQType.getBytes());
                }
                else
                {
                    conn.execute("INSERT INTO pals_question_types (uuid_qtype, uuid_plugin, title, description) VALUES(?,?,?,?);", uuidQType.getBytes(), uuidPlugin.getBytes(), title, description);
                    persisted = true;
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
     * Persists the criteria for this question-type; this is executed within
     * a transaction.
     * 
     * @param conn Database connector.
     * @return The status of the operation.
     */
    public CriteriaPersistStatus persistCriterias(Connector conn)
    {
        try
        {
            // Begin transaction
            conn.execute("BEGIN;");
            // Delete existing criteria
            conn.execute("DELETE FROM pals_qtype_ctype WHERE uuid_qtype=?;", uuidQType.getBytes());
            // Insert each criteria
            for(Map.Entry<UUID,TypeCriteria> kv : criterias.entrySet())
            {
                conn.execute("INSERT INTO pals_qtype_ctype (uuid_qtype, uuid_ctype) VALUES (?,?);", uuidQType.getBytes(), kv.getKey().getBytes());
            }
            // Commit the changes of the transaction
            conn.execute("COMMIT;");
            return CriteriaPersistStatus.Success;
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
            return CriteriaPersistStatus.Failed;
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
        if(uuidQType == null || !persisted)
            return false;
        try
        {
            conn.execute("DELETE FROM pals_question_types WHERE uuid_qtype=?;", uuidQType.getBytes());
            persisted = false;
            return true;
        }
        catch(DatabaseException ex)
        {
            return false;
        }
    }
    // Methods - Mutators ******************************************************
    /**
     * @param uuidQType Sets the UUID for this model; cannot be null. This will
     * not work for persisted models.
     */
    public void setUuidQType(UUID uuidQType)
    {
        if(!persisted)
            this.uuidQType = uuidQType;
    }
    /**
     * @param uuidPlugin Sets the plugin which owns this model; cannot be null.
     */
    public void setUuidPlugin(UUID uuidPlugin)
    {
        this.uuidPlugin = uuidPlugin;
    }
    /**
     * @param title Sets the title for this model; cannot be null.
     */
    public void setTitle(String title)
    {
        this.title = title;
    }
    /**
     * @param description Sets the description for this model; cannot be null.
     */
    public void setDescription(String description)
    {
        this.description = description;
    }
    // Methods - Mutators - Criterias ******************************************
    public void criteriaAdd(TypeCriteria criteria)
    {
        criterias.put(criteria.getUuidCType(), criteria);
    }
    public void criteriaRemove(TypeCriteria criteria)
    {
        criterias.remove(criteria.getUuidCType());
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
     * @return The UUID of this model.
     */
    public UUID getUuidQType()
    {
        return uuidQType;
    }
    /**
     * @return The UUID of the plugin which owns this model.
     */
    public UUID getUuidPlugin()
    {
        return uuidPlugin;
    }
    /**
     * @return The title of this model.
     */
    public String getTitle()
    {
        return title;
    }
    /**
     * @return The description of this model.
     */
    public String getDescription()
    {
        return description;
    }
    /**
     * @return The map used to hold the critera-types for this type of
     * question.
     */
    public HashMap<UUID,TypeCriteria> getCriteriasMap()
    {
        return criterias;
    }
    /**
     * @return Array of criteria-types; can be empty.
     */
    public TypeCriteria[] getCriterias()
    {
        return criterias.values().toArray(new TypeCriteria[criterias.size()]);
    }
    // Methods - Accessors - Limits ********************************************
    /**
     * @return The minimum length of a title.
     */
    public int getTitleMin()
    {
        return 1;
    }
    /**
     * @return The maximum length of a title.
     */
    public int getTitleMax()
    {
        return 64;
    }
}
