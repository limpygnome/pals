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
package pals.plugin_reloader.models;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import pals.base.PluginManager;
import pals.base.rmi.RMI_Interface;
import pals.base.UUID;
import pals.base.Version;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.database.Result;

/**
 * Represents a plugin. This model is used since it contains more information
 * than the plugin model in the base framework. However, a node may also
 * not have all the plugins loaded locally; therefore plugins have to be
 * queried.
 */
public class ModelPlugin
{
    // Fields ******************************************************************
    private UUID    uuid;
    private String  title;
    private boolean system;
    private Version version;
    private int     state;
    // Methods - Constructors **************************************************
    private ModelPlugin(UUID uuid, String title, boolean system, Version version, int state)
    {
        this.uuid = uuid;
        this.title = title;
        this.system = system;
        this.version = version;
        this.state = state;
    }
    // Methods - Persistence ***************************************************
    /**
     * Loads all of the plugins on the system (globally - not just the
     * current node).
     * 
     * @param conn Database connector.
     * @return Array of models; can be empty.
     */
    public static ModelPlugin[] load(Connector conn)
    {
        try
        {
            Result res = conn.read("SELECT * FROM pals_plugins ORDER BY title ASC;");
            ArrayList<ModelPlugin> buffer = new ArrayList<>();
            ModelPlugin t;
            while(res.next())
            {
                if((t = load(res)) != null)
                    buffer.add(t);
            }
            return buffer.toArray(new ModelPlugin[buffer.size()]);
        }
        catch(DatabaseException ex)
        {
            return new ModelPlugin[0];
        }
    }
    /**
     * @param conn Database connector.
     * @param uuid The UUID of the plugin.
     * @return An instance of a model or null.
     */
    public static ModelPlugin load(Connector conn, UUID uuid)
    {
        try
        {
            Result res = conn.read("SELECT * FROM pals_plugins WHERE uuid_plugin=?;", uuid.getBytes());
            return res.next() ? load(res) : null;
        }
        catch(DatabaseException ex)
        {
            return null;
        }
    }
    private static ModelPlugin load(Result res)
    {
        try
        {
            return new ModelPlugin(UUID.parse((byte[])res.get("uuid_plugin")), (String)res.get("title"), ((String)res.get("system")).equals("1"), new Version((int)res.get("version_major"), (int)res.get("version_minor"), (int)res.get("version_build")), (int)res.get("state"));
        }
        catch(DatabaseException ex)
        {
            return null;
        }
    }
    /**
     * Deletes a plugin from the database.
     * 
     * @param conn Database connector.
     * @param plugin The plugin to be deleted.
     */
    public static void pluginDelete(Connector conn, ModelPlugin plugin)
    {
        try
        {
            conn.execute("DELETE FROM pals_plugins WHERE uuid_plugin=?;", plugin.uuid.getBytes());
        }
        catch(DatabaseException ex)
        {
        }
    }
    /**
     * Sets a plugin to be uninstalled.
     * 
     * @param conn Database connector.
     * @param plugin The plugin to be uninstalled.
     */
    public static void pluginUninstall(Connector conn, ModelPlugin plugin)
    {
        try
        {
            conn.execute("UPDATE pals_plugins SET state=? WHERE uuid_plugin=?;", PluginManager.DbPluginState.PendingUninstall.getDbVal(), plugin.uuid.getBytes());
        }
        catch(DatabaseException ex)
        {
        }
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The identifier of the plugin.
     */
    public UUID getUUID()
    {
        return uuid;
    }
    /**
     * @return The title of the plugin.
     */
    public String getTitle()
    {
        return title;
    }
    /**
     * @return Indicates if the plugin is a system plugin.
     */
    public boolean isSystem()
    {
        return system;
    }
    /**
     * @return The version of the plugin.
     */
    public Version getVersion()
    {
        return version;
    }
    /**
     * @return The state of the plugin.
     */
    public int getState()
    {
        return state;
    }
    // Methods - Static ********************************************************
    /**
     * Unloads a plugin from all of the nodes.
     * 
     * @param conn Database connector.
     * @param plugin The plugin to be unloaded from all the nodes.
     */
    public static void pluginUnload(Connector conn, ModelPlugin plugin)
    {
        try
        {
            Result res = conn.read("SELECT rmi_ip, rmi_port FROM pals_nodes WHERE rmi_ip IS NOT NULL AND rmi_port IS NOT NULL;");
            Registry r;
            RMI_Interface ri;
            while(res.next())
            {
                try
                {
                    r = LocateRegistry.getRegistry((String)res.get("rmi_ip"), (int)res.get("rmi_port"));
                    ri = (RMI_Interface)r.lookup(RMI_Interface.class.getName());
                    ri.pluginUnload(plugin.uuid);
                }
                catch(DatabaseException | NotBoundException | RemoteException ex)
                {
                }
            }
        }
        catch(DatabaseException ex)
        {
        }
    }
}
