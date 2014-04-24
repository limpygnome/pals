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
import java.util.HashMap;
import java.util.Map;
import pals.base.Logging;
import pals.base.NodeCore;
import pals.base.Plugin;
import pals.base.UUID;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.database.Result;

/**
 * A model to represent a type of question.
 * 
 * @version 1.0
 */
public class TypeQuestion
{
    // Enums *******************************************************************
    /**
     * The status from attempting to persist the model.
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
         * Invalid identifier.
         * 
         * @since 1.0
         */
        Invalid_UUID,
        /**
         * Invalid plugin identifier.
         * 
         * @since 1.0
         */
        Invalid_PluginUUID,
        /**
         * Invalid title.
         * 
         * @since 1.0
         */
        Invalid_Title,
        /**
         * Invalid description.
         * 
         * @since 1.0
         */
        Invalid_Description,
    }
    /**
     * Criteria persist status from persisting type-criterias for
     * a type question.
     * 
     * Refer to DefaultQC.java in DefaultQC plugin.
     * 
     * @since 1.0
     */
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
    /**
     * Constructs new instance of type question.
     * 
     * @since 1.0
     */
    public TypeQuestion()
    {
        this(null, null, null, null);
    }
    /**
     * Constructs new instance of type question.
     * 
     * @param uuidQType The identifier.
     * @param uuidPlugin The plugin identifier.
     * @param title The title.
     * @param description The description.
     * @since 1.0
     */
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
     * @since 1.0
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
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return new TypeQuestion[0];
        }
    }
    /**
     * Loads a persisted model from the database.
     * 
     * @param conn Database connector.
     * @param uuidQType The UUID of the model.
     * @return An instance of the model or null.
     * @since 1.0
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
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return null;
        }
    }
    /**
     * Loads a persisted model from a result.
     * 
     * @param result The result with the data; next() should be pe-invoked.
     * @return An instance of the model or null.
     * @since 1.0
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
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return null;
        }
    }
    /**
     * Loads the criteria-types available for this type of question; by default,
     * these are not loaded.
     * 
     * @param conn Database connector.
     * @return True = loaded, false = failed.
     * @since 1.0
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
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return false;
        }
    }
    /**
     * Persists the model.
     * 
     * @param conn Database connector.
     * @return Status from the operation.
     * @since 1.0
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
                NodeCore core;
                if((core = NodeCore.getInstance())!=null)
                    core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
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
     * @since 1.0
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
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return CriteriaPersistStatus.Failed;
        }
    }
    /**
     * Removes the model from the database.
     * 
     * @param conn Database connector.
     * @return True = removed, false = failed.
     * @since 1.0
     */
    public boolean delete(Connector conn)
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
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return false;
        }
    }
    // Methods - Mutators ******************************************************
    /**
     * Sets the identifier for this question type.
     * 
     * @param uuidQType Sets the UUID for this model; cannot be null. This will
     * not work for persisted models.
     * @since 1.0
     */
    public void setUuidQType(UUID uuidQType)
    {
        if(!persisted)
            this.uuidQType = uuidQType;
    }
    /**
     * Sets the identifier of the plugin responsible for this type of question.
     * 
     * @param uuidPlugin Sets the plugin which owns this model; cannot be null.
     * @since 1.0
     */
    public void setUuidPlugin(UUID uuidPlugin)
    {
        this.uuidPlugin = uuidPlugin;
    }
    /**
     * Sets the title.
     * 
     * @param title Sets the title for this model; cannot be null.
     * @since 1.0
     */
    public void setTitle(String title)
    {
        this.title = title;
    }
    /**
     * Sets the description.
     * 
     * @param description Sets the description for this model; cannot be null.
     * @since 1.0
     */
    public void setDescription(String description)
    {
        this.description = description;
    }
    // Methods - Mutators - Criterias ******************************************
    /**
     * Adds a criteria-type compatible with this question-type.
     * 
     * @param criteria The type of criteria compatible.
     * @since 1.0
     */
    public void criteriaAdd(TypeCriteria criteria)
    {
        criterias.put(criteria.getUuidCType(), criteria);
    }
    /**
     * Removes a compatible riteria-type from this question-type.
     * 
     * @param criteria The criteria.
     * @since 1.0
     */
    public void criteriaRemove(TypeCriteria criteria)
    {
        criterias.remove(criteria.getUuidCType());
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
        return persisted;
    }
    /**
     * The question identifier.
     * 
     * @return The UUID of this model.
     * @since 1.0
     */
    public UUID getUuidQType()
    {
        return uuidQType;
    }
    /**
     * The plugin identifier.
     * 
     * @return The UUID of the plugin which owns this model.
     * @since 1.0
     */
    public UUID getUuidPlugin()
    {
        return uuidPlugin;
    }
    /**
     * The title.
     * 
     * @return The title of this model.
     * @since 1.0
     */
    public String getTitle()
    {
        return title;
    }
    /**
     * The description.
     * 
     * @return The description of this model.
     * @since 1.0
     */
    public String getDescription()
    {
        return description;
    }
    /**
     * Underlying map for storing compatible criteria-types.
     * 
     * @return The map used to hold the critera-types for this type of
     * question.
     * @since 1.0
     */
    public HashMap<UUID,TypeCriteria> getCriteriasMap()
    {
        return criterias;
    }
    /**
     * An array of compatible criteria-types.
     * 
     * @return Array of criteria-types; can be empty.
     * @since 1.0
     */
    public TypeCriteria[] getCriterias()
    {
        return criterias.values().toArray(new TypeCriteria[criterias.size()]);
    }
    // Methods - Accessors - Limits ********************************************
    /**
     * The minimum length of a title.
     * 
     * @return The minimum length of a title.
     * @since 1.0
     */
    public int getTitleMin()
    {
        return 1;
    }
    /**
     * The maximum length of a title.
     * 
     * @return The maximum length of a title.
     * @since 1.0
     */
    public int getTitleMax()
    {
        return 64;
    }
    // Methods - Static ********************************************************
    /**
     * Registers a new type of question. If a type already exists with the same
     * UUID, it's loaded and returned and no changes will occur on the
     * database.
     * 
     * @param conn Database connector.
     * @param core The current instance of the core.
     * @param plugin The plugin which owns the type.
     * @param uuid The identifier of the type.
     * @param title The title of the type.
     * @param description A description for the type.
     * @return Instance of type. Can be null if type cannot be persisted.
     * @since 1.0
     */
    public static TypeQuestion register(Connector conn, NodeCore core, Plugin plugin, UUID uuid, String title, String description)
    {
        TypeQuestion tq = load(conn, uuid);
        // Attempt to load former
        if(tq != null)
            return tq;
        // Create new
        tq = new TypeQuestion(uuid, plugin.getUUID(), title, description);
        TypeQuestion.PersistStatus psq = tq.persist(conn);
        if(psq != TypeQuestion.PersistStatus.Success)
        {
            core.getLogging().log("Base.TypeQuestion#register", "Failed to register type-question '"+title+"' during installation!", Logging.EntryType.Error);
            return null;
        }
        return tq;
    }
    /**
     * Unregisters a type of question.
     * 
     * @param conn Database connector.
     * @param uuid The identifier of the type.
     * @return Indicates if the operation has succeeded.
     * @since 1.0
     */
    public static boolean unregister(Connector conn, UUID uuid)
    {
        TypeQuestion tq = TypeQuestion.load(conn, uuid);
        if(tq != null)
            return tq.delete(conn);
        return false;
    }
    // Methods - Overrides *****************************************************
    /**
     * Tests if an object is equal to this instance. This is based on both
     * objects being of this type and having the same plugin and type UUID.
     * 
     * @param o The object to be tested.
     * @return True = equal, false = not equal.
     * @since 1.0
     */
    @Override
    public boolean equals(Object o)
    {
        if(o == null)
            return false;
        else if(uuidPlugin == null || uuidQType == null)
            return false;
        else if(!(o instanceof TypeQuestion))
            return false;
        TypeQuestion tq = (TypeQuestion)o;
        return uuidPlugin.equals(tq.getUuidPlugin()) && uuidQType.equals(tq.getUuidQType());
    }
    /**
     * Hash-code is based on the hash-code of the plugin UUID.
     * 
     * @return Hash-code.
     * @since 1.0
     */
    @Override
    public int hashCode()
    {
        return uuidPlugin == null ? 0 : uuidPlugin.hashCode();
    }
}
