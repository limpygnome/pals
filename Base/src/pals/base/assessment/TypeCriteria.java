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
import pals.base.Logging;
import pals.base.NodeCore;
import pals.base.Plugin;
import pals.base.UUID;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.database.Result;

/**
 * A model to represent the criteria type, for a question.
 * 
 * @version 1.0
 */
public class TypeCriteria
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
         * Invalid plugin UUID.
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
    // Fields ******************************************************************
    private boolean persisted;
    private UUID    uuidCType;
    private UUID    uuidPlugin;
    private String  title;
    private String  description;
    // Methods - Constructors **************************************************
    /**
     * Constructs a new type of criteria.
     * 
     * @since 1.0
     */
    public TypeCriteria()
    {
        this(null, null, null, null);
    }
    /**
     * Constructs a new type of criteria.
     * 
     * @param uuidCType The identifier of the criteria.
     * @param uuidPlugin The identifier of the plugin, which is responsible
     * for handling the criteria.
     * @param title The title of the criteria.
     * @param description A description of the criteria.
     * @since 1.0
     */
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
     * @since 1.0
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
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return new TypeCriteria[0];
        }
    }
    /**
     * Loads a persisted model from the database.
     * 
     * @param conn Database connector.
     * @param uuidCType The UUID of the model.
     * @return An instance of the model or null.
     * @since 1.0
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
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return null;
        }
    }
    /**
     * Loads a persisted model from a result; next() should be pre-invoked.
     * 
     * @param result The result with the data; next() should be pre-invoked.
     * @return An instance of the model or null.
     * @since 1.0
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
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return null;
        }
    }
    /**
     * Persists the model.
     * 
     * @param conn Database connector.
     * @return Status from the operation.
     * @since 1.0
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
                NodeCore core;
                if((core = NodeCore.getInstance())!=null)
                    core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
                return TypeCriteria.PersistStatus.Failed;
            }
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
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return false;
        }
    }
    // Methods - Mutators ******************************************************
    /**
     * Sets the identifier of the criteria.
     * 
     * @param uuidCType The new identifier; cannot be null.
     * @since 1.0
     */
    public void setUuidCType(UUID uuidCType)
    {
        this.uuidCType = uuidCType;
    }
    /**
     * Sets the plugin which handles and owns this criteria.
     * 
     * @param uuidPlugin Plugin identifier; cannot be null.
     * @since 1.0
     */
    public void setUuidPlugin(UUID uuidPlugin)
    {
        this.uuidPlugin = uuidPlugin;
    }
    /**
     * Sets the title.
     * 
     * @param title The title for this model; cannot be null.
     * @since 1.0
     */
    public void setTitle(String title)
    {
        this.title = title;
    }
    /**
     * Sets the description.
     * 
     * @param description The description; cannot be null.
     * @since 1.0
     */
    public void setDescription(String description)
    {
        this.description = description;
    }
    // Methods - Accessors *****************************************************
    /**
     * Indicates if the model has been persisted.
     * 
     * @return True = persisted, false = not persisted.
     * @since 1.0
     */
    public boolean isPersisted()
    {
        return persisted;
    }
    /**
     * The UUID identifier.
     * 
     * @return The UUID of this model.
     * @since 1.0
     */
    public UUID getUuidCType()
    {
        return uuidCType;
    }
    /**
     * The UUID of the plugin.
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
    // Methods - Accessors - Static ********************************************
    /**
     * Indicates if a type of criteria is able to serve a type of question.
     * 
     * @param conn Database connector.
     * @param qt Type of question.
     * @param ct Type of criteria.
     * @return True = capable, false = not capable.
     * @since 1.0
     */
    public static boolean isCapable(Connector conn, TypeQuestion qt, TypeCriteria ct)
    {
        try
        {
            return ((long)conn.executeScalar("SELECT COUNT('') FROM pals_qtype_ctype WHERE uuid_qtype=? AND uuid_ctype=?;", qt.getUuidQType().getBytes(), ct.getUuidCType().getBytes())) > 0;
        }
        catch(DatabaseException ex)
        {
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return false;
        }
    }
    // Methods - Static ********************************************************
    /**
     * Registers a new criteria type. If a type already exists with the same
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
    public static TypeCriteria register(Connector conn, NodeCore core, Plugin plugin, UUID uuid, String title, String description)
    {
        TypeCriteria tc = new TypeCriteria(uuid, plugin.getUUID(), title, description);
        TypeCriteria.PersistStatus psc = tc.persist(conn);
        if(psc != TypeCriteria.PersistStatus.Success)
        {
            core.getLogging().log("Base.TypeCriteria#register", "Failed to register type-criteria '"+title+"' during installation!", Logging.EntryType.Error);
            return null;
        }
        return tc;
    }
    /**
     * Unregisters a type of criteria.
     * 
     * @param conn Database connector.
     * @param uuid The identifier of the type.
     * @return Indicates if the operation has succeeded.
     * @since 1.0
     */
    public static boolean unregister(Connector conn, UUID uuid)
    {
        TypeCriteria tc = TypeCriteria.load(conn, uuid);
        if(tc != null)
            return tc.delete(conn);
        return false;
    }
}
