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
package pals.base;

import pals.base.rmi.RMI_DefaultServer;
import pals.base.rmi.RMI;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.EnumSet;
import java.util.Random;
import org.apache.commons.io.FileUtils;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.database.Result;
import pals.base.database.connectors.*;
import pals.base.utils.Misc;

/**
 * A class used for an instance of a node; this is responsible for node features
 * such as plugin management, logging and node intercommunication.
 * 
 * Only one NodeCore may run; singleton design pattern enforced.
 * 
 * @version 1.0
 */
public class NodeCore
{
    // Enums *******************************************************************
    /**
     * The state of the core.
     * 
     * The core can be in different states, which indicate the runtime
     * operation mode of the base system.
     * 
     * @since 1.0
     */
    public enum State
    {
        /**
         * Indicates the core is starting.
         * 
         * @since 1.0
         */
        Starting,
        /**
         * Indicates the core has started/is running.
         * 
         * @since 1.0
         */
        Started,
        /**
         * Indicates the core is stopping.
         * 
         * @since 1.0
         */
        Stopping,
        /**
         * Indicates the core has stopped.
         * 
         * @since 1.0
         */
        Stopped,
        /**
         * Indicates the core has failed and is not running.
         * 
         * @since 1.0
         */
        Failed,
        /**
         * Indicates the core has shutdown.
         * 
         * @since 1.0
         */
        Shutdown
    }
    /**
     * The type of core stop to occur.
     * 
     * @since 1.0
     */
    public enum StopType
    {
        /**
         * Indicates the core should just go into a normal stop state.
         * 
         * @since 1.0
         */
        Normal,
        /**
         * Indicates the core should go into a failure/failed state.
         * 
         * @since 1.0
         */
        Failure,
        /**
         * Indicates the core should go to a shutdown state; useful for sending
         * a signal to daemon processes to cease execution.
         * 
         * @since 1.0
         */
        Shutdown
    }
    // Fields - Constants ******************************************************
    private static final String LOGGING_ALIAS_START = "PALS CORE START";
    private static final String LOGGING_ALIAS_STOP  = "PALS CORE STOP";
    private static final long   STATE_CHANGE_TIMEOUT = 10000L;
    // Fields - Instance *******************************************************
    private static NodeCore     currentInstance = null;             // The current instance of the NodeCore.
    // Fields ******************************************************************
    private UUID                uuidNode;                           // The UUID used to represent this node.
    private State               state;                              // The current state of the core.
    private String              pathSettings;                       // The path of the settings file.
    private String              pathPlugins;                        // The path of where the plugins reside.
    private String              pathShared;                         // The path of shared storage.
    private Random              rng;                                // A random number generator (RNG) for general random usage; avoids multiple instances.
    // Fields - Core Components ************************************************
    private PluginManager       plugins;                            // A collection of plugins for the system.
    private TemplateManager     templates;                          // A collection of templates for the system.
    private WebManager          web;                                // Handles web-requests.
    private Logging             logging;                            // Logging of system events.
    private Settings            settings;                           // Read-only core settings loaded from file.
    private RMI                 comms;                              // RMI communications.
    // Methods - Constructors **************************************************
    private NodeCore()
    {
        this.uuidNode = null;
        this.pathPlugins = this.pathShared = null;
        this.pathSettings = "_config/node.config"; // Default path.
        this.state = State.Stopped;
        this.plugins = null;
        this.templates = null;
        this.web = null;
        this.logging = null;
        this.settings = null;
        this.comms = null;
        this.rng = null;
    }
    // Methods - Core **********************************************************
    /**
     * Starts the core of the node.
     * 
     * @return True = started, false = failed.
     * @since 1.0
     */
    public synchronized boolean start()
    {
        // Check the core is in either the stopped or failed state to start
        if(state != State.Failed && state != State.Stopped)
            return false;
        // Update the state to starting...
        state = State.Starting;
        // Notify any threads
        notifyAll();
        // Initialize RNG
        rng = new Random(System.currentTimeMillis());
        // Load base node settings
        try
        {
            settings = Settings.load(pathSettings, true);
        }
        catch(SettingsException ex)
        {
            System.err.println(LOGGING_ALIAS_START+" Failed to node.config settings - '" + ex.getExceptionType().toString() + "' ~ '" + ex.getMessage() + "'.");
            stop(StopType.Failure);
            return false;
        }
        System.out.println(LOGGING_ALIAS_START+" Loaded settings.");
        // Parse the UUID for this node
        uuidNode = UUID.parse(settings.getStr("node/uuid"));
        if(uuidNode == null)
        {
            System.err.println(LOGGING_ALIAS_START+" Failed to parse UUID for this node (settings: node/uuid) ~ data: '" + settings.getStr("node/uuid") + "'!");
            stop(StopType.Failure);
            return false;
        }
        else
            System.out.println(LOGGING_ALIAS_START+" Node identifier: '" + uuidNode.getHexHyphens() + "'.");
        // Check the storage path exists and we have read/write permissions
        {
            pathShared = settings.getStr("storage/path");
            // Validate setting
            if(pathShared == null || pathShared.length() == 0)
            {
                System.err.println(LOGGING_ALIAS_START+" Setting 'storage/path' is missing or invalid!");
                stop(StopType.Failure);
                return false;
            }
            // Validate access to path
            Storage.StorageAccess access = Storage.checkAccess(pathShared, true, true, true, true);
            switch(access)
            {
                case DoesNotExist:
                    System.err.println(LOGGING_ALIAS_START+" Setting 'storage/path', with value '" + pathShared + "': path does not exist!");
                    stop(StopType.Failure);
                    return false;
                case CannotRead:
                    System.err.println(LOGGING_ALIAS_START+" Setting 'storage/path', with value '" + pathShared + "': no read permissions!");
                    stop(StopType.Failure);
                    return false;
                case CannotWrite:
                    System.err.println(LOGGING_ALIAS_START+" Setting 'storage/path', with value '" + pathShared + "': no write permissions!");
                    stop(StopType.Failure);
                    return false;
                case CannotExecute:
                    System.err.println(LOGGING_ALIAS_START+" Setting 'storage/path', with value '" + pathShared + "': no execute permissions!");
                    stop(StopType.Failure);
                    return false;
            }
            // Log the storage path
            File storage = new File(pathShared);
            try
            {
                System.out.println(LOGGING_ALIAS_START+" Storage path '" + storage.getCanonicalPath() + "' checked, with r+w+e permissions.");
            }
            catch(IOException ex)
            {
                System.err.println(LOGGING_ALIAS_START+" Storage path '" + storage.getPath() + "' (#2) checked, with r+w+e permissions.");
            }
        }
        // Ensure shared-path sub-dirs are created
        if(!Storage.checkFoldersCreated(pathShared))
        {
            System.err.println(LOGGING_ALIAS_START+" Failed to ensure shared storage sub-directories have been created at '"+pathShared+"'!");
            stop(StopType.Failure);
            return false;
        }
        // Start logging
        {
            EnumSet loggingTypes = Logging.EntryType.getSet(settings.getStr("node/logging_types"));
            if(loggingTypes == null)
            {
                System.err.println(LOGGING_ALIAS_START+" Invalid logging types specified on config (setting: node/logging_types), value: '" + settings.getStr("node/logging_types") + "'!");
                stop(StopType.Failure);
                return false;
            }
            if((logging = Logging.createInstance(this, "system", true, loggingTypes)) == null)
            {
                System.err.println(LOGGING_ALIAS_START+" Failed to start core logging, aborted!");
                stop(StopType.Failure);
                return false;
            }
            logging.log(LOGGING_ALIAS_START, "Started logging.", Logging.EntryType.Info);
        }
        // Ensure temp_plugins exists for plugins
        {
            File tempPlugins = new File(getPathPlugins_Temp());
            // Delete the directory if it already exists (we'll recreate it)
            if(tempPlugins.exists())
            {
                try
                {
                    FileUtils.deleteDirectory(tempPlugins);
                }
                catch(IOException ex)
                {
                    logging.logEx(LOGGING_ALIAS_START, "Failed to delete temporary directory for plugins ["+tempPlugins.getAbsolutePath()+"]!", ex, Logging.EntryType.Warning);
                }
            }
            // (Re)create directory
            if(!tempPlugins.mkdir())
            {
                logging.log(LOGGING_ALIAS_START, "Failed to create temporary directory for plugins ["+tempPlugins.getAbsolutePath()+"]!", Logging.EntryType.Error);
                stop(StopType.Failure);
                return false;
            }
        }
        // Create an initial connection to the database
        Connector conn = createConnector();
        if(conn == null)
        {
            logging.log(LOGGING_ALIAS_START, "Failed to create database connector.", Logging.EntryType.Error);
            stop(StopType.Failure);
            return false;
        }
        else
            logging.log(LOGGING_ALIAS_START, "Established database connection.", Logging.EntryType.Info);
        // Perform node SQL operations
        try
        {
            // Check if the initial database setup is needed
            // -- DBMS dependent SQL here!
            if(!((boolean)conn.executeScalar("SELECT EXISTS(SELECT * FROM information_schema.tables WHERE table_name='pals_nodes');")))
            {
                logging.log(LOGGING_ALIAS_START, "Setting up initial database...", Logging.EntryType.Info);
                // Fetch SQL for initial database setup
                Misc.executeSqlFile(new File("_sql/install.sql"), conn);
                logging.log(LOGGING_ALIAS_START, "Successfully setup initial database.", Logging.EntryType.Info);
            }
            // Check this node exists in the database, else create the record
            // -- Important for other nodes to contact us
            String nodeTitle = settings.getStr("node/title", "Untitled Node");
            Result res = conn.read("SELECT title FROM pals_nodes WHERE uuid_node=?;", uuidNode.getBytes());
            if(!res.next())
            {
                conn.execute("INSERT INTO pals_nodes (uuid_node,title,last_active) VALUES(?,?,current_timestamp);", uuidNode.getBytes(), nodeTitle);
                logging.log(LOGGING_ALIAS_START, "Added node to database.", Logging.EntryType.Info);
            }
            // Update title and RMI information for this node
            try
            {
                conn.execute("UPDATE pals_nodes SET title=?, rmi_ip=?, rmi_port=? WHERE uuid_node=?", nodeTitle, InetAddress.getLocalHost().getHostAddress(), settings.getInt("rmi/port", 1099), uuidNode.getBytes());
            }
            catch(UnknownHostException ex)
            {
                logging.logEx(LOGGING_ALIAS_START, "Could not update RMI information - address could not be found for the local host!", ex, Logging.EntryType.Warning);
            }
        }
        catch(IOException ex)
        {
            logging.logEx(LOGGING_ALIAS_START, "Failed to setup initial database.", ex, Logging.EntryType.Error);
            stop(StopType.Failure);
            return false;
        }
        catch(DatabaseException ex)
        {
            logging.logEx(LOGGING_ALIAS_START, "Failed to check existence of node in database.", ex, Logging.EntryType.Error);
            stop(StopType.Failure);
            return false;
        }
        // Initialize the templates manager, load the required templates
        templates = new TemplateManager(this);
        logging.log(LOGGING_ALIAS_START, "Initialized templates.", Logging.EntryType.Info);
        // Load templates from shared folder
        {
            String pathTemplates = Storage.getPath_templates(pathShared);
            File dirTemlates = new File(pathTemplates);
            if(dirTemlates.exists() && dirTemlates.isDirectory())
            {
                if(!templates.loadDir(null, pathTemplates))
                {
                    logging.log(LOGGING_ALIAS_START, "Failed to load shared storage templates at '" + dirTemlates.getPath() + "'!", Logging.EntryType.Error);
                    stop(StopType.Failure);
                    return false;
                }
                else
                    logging.log(LOGGING_ALIAS_START, "Loaded shared storage templates.", Logging.EntryType.Info);
            }
            else
            {
                dirTemlates.mkdir();
                logging.log(LOGGING_ALIAS_START, "Created templates directory in shared file storage.", Logging.EntryType.Info);
            }
        }
        // Initialize web manager
        web = new WebManager(this);
        logging.log(LOGGING_ALIAS_START, "Initialized web manager.", Logging.EntryType.Info);
        // Initialize plugin manager, load all the plugins
        plugins = new PluginManager(this);
        // -- Attempt to load the plugins path from settings, if it exists
        // -- -- Unless path has already been specified
        if(pathPlugins == null)
        {
            try
            {
                setPathPlugins((String)settings.get2("plugins/path"));
            }
            catch(SettingsException ex)
            {
                // Does not exist - ignore and use default internal setting!
            }
        }
        if(!plugins.reload(conn))
        {
            logging.log(LOGGING_ALIAS_START, "Failed to load plugins.", Logging.EntryType.Error);
            stop(StopType.Failure);
            return false;
        }
        logging.log(LOGGING_ALIAS_START, "Loaded plugins.", Logging.EntryType.Info);
        // Setup comms
        int rmiPort = settings.getInt("rmi/port", 1099);
        try
        {
            comms = new RMI(this, conn, rmiPort, new RMI_DefaultServer(this));
            if(!comms.start())
                throw new Exception("Could not setup RMI socket.");
        }
        catch(Exception ex)
        {
            logging.logEx(LOGGING_ALIAS_START, "Failed to setup RMI server.", ex, Logging.EntryType.Error);
            stop(StopType.Failure);
            return false;
        }
        // Dispose connector
        conn.disconnect();
        logging.log(LOGGING_ALIAS_START, "Started RMI service on port '" + rmiPort + "'.", Logging.EntryType.Info);
        logging.log(LOGGING_ALIAS_START, "Core started.", Logging.EntryType.Info);
        // Update the state to started
        state = State.Started;
        // Notify any threads
        notifyAll();
        return true;
    }
    /**
     * Stops the core of the node (normal state).
     * 
     * @return True = stopped, false = no changes.
     * @since 1.0
     */
    public synchronized boolean stop()
    {
        return stop(StopType.Normal);
    }
    /**
     * Stops the core of the node.
     * 
     * @param type The type of core-stop to occur.
     * @return True = stopped, false = no changes.
     * @since 1.0
     */
    public synchronized boolean stop(StopType type)
    {
        if(state != State.Started && state != State.Starting)
            return false;
        state = State.Stopping;
        logging.log(LOGGING_ALIAS_STOP, "Stopping core...", Logging.EntryType.Info);
        // Notify any threads
        notifyAll();
        // Dispose RMI/comms
        if(comms != null)
        {
            comms.stop();
            comms = null;
        }
        logging.log(LOGGING_ALIAS_STOP, "Disposed RMI...", Logging.EntryType.Info);
        // Unload all the plugins
        if(plugins != null)
        {
            plugins.unload();
            plugins = null;
        }
        logging.log(LOGGING_ALIAS_STOP, "Disposed plugins...", Logging.EntryType.Info);
        // Unload all the templates
        if(templates != null)
        {
            templates.clear();
            templates = null;
        }
        logging.log(LOGGING_ALIAS_STOP, "Disposed templates...", Logging.EntryType.Info);
        // Dispose web-manager
        web = null;
        // Dispose settings
        settings = null;
        // Destroy RNG
        rng = null;
        // Dispose paths
        this.pathShared = null;
        // Decide on new state (and add any appropriate logging)
        State newState;
        switch(type)
        {
            case Failure:
                logging.log(LOGGING_ALIAS_STOP, "Going into failure state...", Logging.EntryType.Info);
                newState = State.Failed;
                break;
            case Shutdown:
                logging.log(LOGGING_ALIAS_STOP, "Going into shutdown state...", Logging.EntryType.Info);
                newState = State.Shutdown;
                break;
            case Normal:
            default:
                newState = State.Stopped;
                break;
        }
        // Enter last entry into log
        logging.log(LOGGING_ALIAS_STOP, "Core stopped.", Logging.EntryType.Info);
        // Unload logging
        if(logging != null)
        {
            logging.dispose(); // This should always be last!
            logging = null;
        }
        // Reset uuid
        uuidNode = null;
        // Update state
        state = newState;
        // Notify any threads
        notifyAll();
        // Output to console
        System.out.println("PALS CORE STOP Core has completely shutdown.");
        return true;
    }
    /**
     * Resets the shutdown state to a stopped state.
     * 
     * @return True = success, false = not changed.
     * @since 1.0
     */
    public synchronized boolean resetShutdown()
    {
        if(state == State.Shutdown)
        {
            state = State.Stopped;
            return true;
        }
        return false;
    }
    // Methods *****************************************************************
    /**
     * Creates a new database connector.
     * 
     * @return Instance, else null if the connector could not be made or
     * connect.
     * @since 1.0
     */
    public Connector createConnector()
    {
        return createConnector(settings);
    }
    /**
     * Creates a new database connector.
     * 
     * @param settings The settings to use to create the connector.
     * @return Instance, else null if the connector could not be made or
     * connect.
     * @since 1.0
     */
    public static Connector createConnector(Settings settings)
    {
        if(settings == null)
            return null;
        
        Connector conn = null;
        // Setup connector based on type
        switch(settings.getInt("database/type"))
        {
            case Postgres.IDENTIFIER_TYPE:
                conn = new Postgres(settings.getStr("database/host"), settings.getStr("database/db"), settings.getStr("database/username"), settings.getStr("database/password"), settings.getInt("database/port"));
                break;
            case MySQL.IDENTIFIER_TYPE:
                conn = new MySQL(settings.getStr("database/host"), settings.getStr("database/db"), settings.getStr("database/username"), settings.getStr("database/password"), settings.getInt("database/port"));
                break;
        }
        // Check we have a valid connector setup
        if(conn == null)
            return null;
        // Connect to the service
        try
        {
            conn.connect();
            return conn;
        }
        catch(DatabaseException ex)
        {
            if(NodeCore.getInstance() != null)
                NodeCore.getInstance().getLogging().logEx(LOGGING_ALIAS_START, "Could not create database connector.", ex, Logging.EntryType.Warning);
            else
                ex.printStackTrace(System.err);
        }
        return null;
    }
    // Methods - Waiting Related ***********************************************
    /**
     * Causes the invoking thread to wait until this object is notified; this
     * will occur when the state of the core changes. The timeout value is
     * dictated by {@link #STATE_CHANGE_TIMEOUT}.
     * 
     * @throws InterruptedException Thrown by Object.wait(); refer to
     * third-party documentation.
     * @since 1.0
     */
    public synchronized void waitStateChange() throws InterruptedException
    {
        wait(STATE_CHANGE_TIMEOUT);
    }
    // Methods - Mutators ******************************************************
    /**
     * Sets the path of plugins.
     * 
     * @param pathPlugins The path of where plugins reside; this will be checked
     * by the plugin-manager when loading all the plugins. If this parameter is
     * incorrect, the plugins will fail to load and the core will fail.
     * @since 1.0
     */
    public synchronized void setPathPlugins(String pathPlugins)
    {
        this.pathPlugins = pathPlugins;
    }
    /**
     * Sets the path of where the settings file resides.
     * 
     * @param pathSettings File path.
     * @since 1.0
     */
    public synchronized void setPathSettings(String pathSettings)
    {
        this.pathSettings = pathSettings;
    }
    // Methods - Accessors *****************************************************
    /**
     * Gets the instance of the current PALS node Core.
     * 
     * @return The current instance of the core.
     * @since 1.0
     */
    public synchronized static NodeCore getInstance()
    {
        if(currentInstance == null)
            currentInstance = new NodeCore();
        return currentInstance;
    }
    /**
     * The current state of the core.
     * 
     * @return The current state of the core.
     * @since 1.0
     */
    public State getState()
    {
        return state;
    }
    /**
     * The path of the plugins.
     * 
     * @return The path of where all the plugin libraries reside.
     * @since 1.0
     */
    public String getPathPlugins()
    {
        return pathPlugins;
    }
    /**
     * The temporary path for the local copy of plugins.
     * 
     * @return The path of where plugins, and their dependencies, are copied
     * to be loaded into the runtime.
     * @since 1.0
     */
    public String getPathPlugins_Temp()
    {
        return "_temp_plugins";
    }
    /**
     * The path for shared files; allows multiple nodes to share files from
     * a central location.
     * 
     * @return The directory of shared files between node(s) and/or website(s).
     * @since 1.0
     */
    public String getPathShared()
    {
        return pathShared;
    }
    /**
     * The path of where the settings file is located.
     * 
     * @return File path.
     * @since 1.0
     */
    public String getPathSettings()
    {
        return pathSettings;
    }
    // Methods - Accessors - Components ****************************************
    /**
     * Fetches the plugin-manager sub-component.
     * 
     * @return The plugin manager responsible for handling the runtime plugins.
     * @since 1.0
     */
    public PluginManager getPlugins()
    {
        return plugins;
    }
    /**
     * Fetches the template-manager sub-component.
     * 
     * @return The template manager responsible for rendering and caching
     * templates.
     * @since 1.0
     */
    public TemplateManager getTemplates()
    {
        return templates;
    }
    /**
     * Fetches the web-manager sub-component.
     * 
     * @return The web manager responsible for handling web-requests.
     * @since 1.0
     */
    public WebManager getWebManager()
    {
        return web;
    }
    /**
     * Fetches the logging sub-component.
     * 
     * @return The logger responsible for logging any events with the core.
     * @since 1.0
     */
    public Logging getLogging()
    {
        return logging;
    }
    /**
     * Fetches read-only settings for this node.
     * 
     * @return Read-only collection of node settings.
     * @since 1.0
     */
    public Settings getSettings()
    {
        return settings;
    }
    /**
     * A shared RNG for the entire system.
     * 
     * @return An instance of the Random class, which is created when the core
     * first starts (seeded with the system time).
     * @since 1.0
     */
    public Random getRNG()
    {
        return rng;
    }
    /**
     * The RMI sub-component, for communication with other nodes.
     * 
     * @return The current instance of the core responsible for handling
     * inter-node communication using RMI.
     * @since 1.0
     */
    public RMI getRMI()
    {
        return comms;
    }
    /**
     * The UUID for this node.
     * 
     * @return The UUID used to identify this node.
     * @since 1.0
     */
    public UUID getNodeUUID()
    {
        return uuidNode;
    }
}
