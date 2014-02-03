package pals.base.assessment;

import java.util.ArrayList;
import pals.base.UUID;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.database.Result;

/**
 * A model to represent the criteria type, for a question.
 */
public class TypeCriteria
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
    // Fields ******************************************************************
    private boolean persisted;
    private UUID    uuidCType;
    private UUID    uuidPlugin;
    private String  title;
    private String  description;
    // Methods - Constructors **************************************************
    public TypeCriteria()
    {
        this(null, null, null, null);
    }
    public TypeCriteria(UUID uuidCType, UUID uuidPlugin, String title, String description)
    {
        this.persisted = false;
        this.uuidCType = uuidCType;
        this.uuidPlugin = uuidPlugin;
        this.title = title;
        this.description = description;
    }

    // Methods - Persistence ***************************************************
    /**
     * Loads all the persisted criteria-types for a question-type.
     * 
     * @param conn Database connector.
     * @param qt Type of question.
     * @return Array of types of questions available.
     */
    public static TypeCriteria[] loadAll(Connector conn, TypeQuestion qt)
    {
        try
        {
            Result res = conn.read("SELECT * FROM pals_criteria_types WHERE uuid_ctype IN (SELECT uuid_ctype FROM pals_qtype_ctype WHERE uuid_qtype=?);", qt.getUuidQType().getBytes());
            TypeCriteria c;
            ArrayList<TypeCriteria> buffer = new ArrayList<>();
            while(res.next())
            {
                if((c = load(res)) != null)
                    buffer.add(c);
            }
            return buffer.toArray(new TypeCriteria[buffer.size()]);
        }
        catch(DatabaseException ex)
        {
            return new TypeCriteria[0];
        }
    }
    /**
     * Loads a persisted model from the database.
     * 
     * @param conn Database connector.
     * @param uuidCType The UUID of the model.
     * @return An instance of the model or null.
     */
    public static TypeCriteria load(Connector conn, UUID uuidCType)
    {
        if(uuidCType == null)
            return null;
        try
        {
            Result res = conn.read("SELECT * FROM pals_criteria_types WHERE uuid_ctype=?;", uuidCType.getBytes());
            return res.next() ? load(res) : null;
        }
        catch(DatabaseException ex)
        {
            return null;
        }
    }
    /**
     * Loads a persisted model from a result; next() should be pre-invoked.
     * 
     * @param result The result with the data; next() should be pe-invoked.
     * @return An instance of the model or null.
     */
    public static TypeCriteria load(Result result)
    {
        try
        {
            TypeCriteria tc = new TypeCriteria(UUID.parse((byte[])result.get("uuid_ctype")), UUID.parse((byte[])result.get("uuid_plugin")), (String)result.get("title"), (String)result.get("description"));
            tc.persisted = true;
            return tc;
        }
        catch(DatabaseException ex)
        {
            return null;
        }
    }
    /**
     * Persists the model.
     * 
     * @param conn Database connector.
     * @return Status from the operation.
     */
    public TypeCriteria.PersistStatus persist(Connector conn)
    {
        // Validate data
        if(uuidCType == null)
            return TypeCriteria.PersistStatus.Invalid_UUID;
        else if(uuidPlugin == null)
            return TypeCriteria.PersistStatus.Invalid_PluginUUID;
        else if(title == null)
            return TypeCriteria.PersistStatus.Invalid_Title;
        else if(description == null || description.length() < 0)
            return TypeCriteria.PersistStatus.Invalid_Description;
        else
        {
            // Attempt to persist data
            try
            {
                if(persisted)
                {
                    conn.execute("UPDATE pals_criteria_types SET uuid_plugin=?, title=?, description=? WHERE uuid_ctype=?;", uuidPlugin.getBytes(), title, description, uuidCType.getBytes());
                }
                else
                {
                    conn.execute("INSERT INTO pals_criteria_types (uuid_ctype, uuid_plugin, title, description) VALUES(?,?,?,?);", uuidCType.getBytes(), uuidPlugin.getBytes(), title, description);
                    persisted = true;
                }
                return TypeCriteria.PersistStatus.Success;
            }
            catch(DatabaseException ex)
            {
                return TypeCriteria.PersistStatus.Failed;
            }
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
        if(uuidCType == null || !persisted)
            return false;
        try
        {
            conn.execute("DELETE FROM pals_criteria_types WHERE uuid_ctype=?;", uuidCType.getBytes());
            persisted = false;
            return true;
        }
        catch(DatabaseException ex)
        {
            return false;
        }
    }
    // Methods - Mutators ******************************************************
    public void setUuidCType(UUID uuidCType)
    {
        this.uuidCType = uuidCType;
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
    public UUID getUuidCType()
    {
        return uuidCType;
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
    // Methods - Accessors - Static ********************************************
    /**
     * Indicates if a type of criteria is able to serve a type of question.
     * 
     * @param conn Database connector.
     * @param qt Type of question.
     * @param ct Type of criteria.
     * @return True = capable, false = not capable.
     */
    public static boolean isCapable(Connector conn, TypeQuestion qt, TypeCriteria ct)
    {
        try
        {
            return ((long)conn.executeScalar("SELECT COUNT('') FROM pals_qtype_ctype WHERE uuid_qtype=? AND uuid_ctype=?;", qt.getUuidQType().getBytes(), ct.getUuidCType().getBytes())) > 0;
        }
        catch(DatabaseException ex)
        {
            return false;
        }
    }
}
