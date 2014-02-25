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
    Version:    1.0
    Authors:    Marcus Craske           <limpygnome@gmail.com>
    ----------------------------------------------------------------------------
*/
package pals.base.assessment;

import java.util.ArrayList;
import pals.base.Logging;
import pals.base.NodeCore;
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
            NodeCore core;
            if((core = NodeCore.getInstance())!=null)
                core.getLogging().logEx("Base", ex, Logging.EntryType.Warning);
            return false;
        }
    }
}
