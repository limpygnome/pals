package pals.base;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Random;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.database.Result;
import pals.base.database.connectors.*;

/**
 * A class used for an instance of a node; this is responsible for node features
 * such as plugin management, logging and node intercommunication.
 * 
 * Only one NodeCore may run; singleton design pattern enforced.
 */
public class NodeCore
{
    // Enums *******************************************************************
    /**
     * The state of the core.
     * 
     * The core can be in different states, which indicate the runtime
     * operation mode of the base system.
     */
    public enum State
    {
        /**
         * Indicates the core is starting.
         */
        Starting,
        /**
         * Indicates the core has started/is running.
         */
        Started,
        /**
         * Indicates the core is stopping.
         */
        Stopping,
        /**
         * Indicates the core has stopped.
         */
        Stopped,
        /**
         * Indicates the core has failed and is not running.
         */
        Failed,
        /**
         * Indicates the core has shutdown.
         */
        Shutdown
    }
    /**
     * The type of core stop to occur.
     */
    public enum StopType
    {
        /**
         * Indicates the core should just go into a normal stop state.
         */
        Normal,
        /**
         * Indicates the core should go into a failure/failed state.
         */
        Failure,
        /**
         * Indicates the core should go to a shutdown state; useful for sending
         * a signal to daemon processes to cease execution.
         */
        Shutdown
    }
    // Fields - Instance *******************************************************
    private static NodeCore     currentInstance = null;             // The current instance of the NodeCore.
    // Fields ******************************************************************
    private UUID                uuidNode;                           // The UUID used to represent this node.
    private State               state;                              // The current state of the core.
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
     * @return True = started, false = failed.
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
            settings = Settings.load("_config/node.config", true);
        }
        catch(SettingsException ex)
        {
            System.err.println("[CORE START] Failed to node.config settings - '" + ex.getExceptionType().toString() + "' ~ '" + ex.getMessage() + "'.");
            stop(StopType.Failure);
            return false;
        }
        System.out.println("[CORE START] Loaded settings.");
        // Parse the UUID for this node
        uuidNode = UUID.parse(settings.getStr("node/uuid"));
        if(uuidNode == null)
        {
            System.err.println("[CORE START] Failed to parse UUID for this node (settings: node/uuid) ~ data: '" + settings.getStr("node/uuid") + "'!");
            stop(StopType.Failure);
            return false;
        }
        else
            System.out.println("[CORE START] Node identifier: '" + uuidNode.getHexHyphens() + "'.");
        // Check the storage path exists and we have read/write permissions
        {
            pathShared = settings.getStr("storage/path");
            // Validate setting
            if(pathShared == null || pathShared.length() == 0)
            {
                System.err.println("[CORE START] Setting 'storage/path' is missing or invalid!");
                stop(StopType.Failure);
                return false;
            }
            // Validate access to path
            Storage.StorageAccess access = Storage.checkAccess(pathShared, true, true, true, true);
            switch(access)
            {
                case DoesNotExist:
                    System.err.println("[CORE START] Setting 'storage/path', with value '" + pathShared + "': path does not exist!");
                    stop(StopType.Failure);
                    return false;
                case CannotRead:
                    System.err.println("[CORE START] Setting 'storage/path', with value '" + pathShared + "': no read permissions!");
                    stop(StopType.Failure);
                    return false;
                case CannotWrite:
                    System.err.println("[CORE START] Setting 'storage/path', with value '" + pathShared + "': no write permissions!");
                    stop(StopType.Failure);
                    return false;
                case CannotExecute:
                    System.err.println("[CORE START] Setting 'storage/path', with value '" + pathShared + "': no execute permissions!");
                    stop(StopType.Failure);
                    return false;
            }
            // Log the storage path
            File storage = new File(pathShared);
            try
            {
                System.out.println("[CORE START] Storage path '" + storage.getCanonicalPath() + "' checked, with r+w+e permissions.");
            }
            catch(IOException ex)
            {
                System.err.println("[CORE START] Storage path '" + storage.getPath() + "' (#2) checked, with r+w+e permissions.");
            }
        }
        // Ensure temp folder exists for web uploads
        {
            File dirTemp = new File(Storage.getPath_webTemp(pathShared));
            if(!dirTemp.exists())
                dirTemp.mkdir();
        }
        // Ensure logs folder exists
        {
            File dirLogs = new File(Storage.getPath_logs(pathShared));
            if(!dirLogs.exists())
                dirLogs.mkdir();
        }
        // Start logging
        {
            EnumSet loggingTypes = Logging.EntryType.getSet(settings.getStr("node/logging_types"));
            if(loggingTypes == null)
            {
                System.err.println("[CORE START] Invalid logging types specified on config (setting: node/logging_types), value: '" + settings.getStr("node/logging_types") + "'!");
                stop(StopType.Failure);
                return false;
            }
            if((logging = Logging.createInstance(this, "system", true, loggingTypes)) == null)
            {
                System.err.println("[CORE START] Failed to start core logging, aborted!");
                stop(StopType.Failure);
                return false;
            }
            logging.log("[CORE START] Started logging.", Logging.EntryType.Info);
        }
        // Create an initial connection to the database
        {
            Connector conn = createConnector();
            if(conn == null)
            {
                logging.log("[CORE START] Failed to create database connector.", Logging.EntryType.Error);
                stop(StopType.Failure);
                return false;
            }
            else
                logging.log("[CORE START] Established database connection.", Logging.EntryType.Info);
            // Check this node exists in the database, else create the record
            try
            {
                long result = (long)conn.executeScalar("SELECT COUNT('') FROM pals_nodes WHERE uuid_node=?;", uuidNode.getBytes());
                if(result == 0)
                {
                    conn.execute("INSERT INTO pals_nodes (uuid_node,last_active) VALUES(?,current_timestamp);", uuidNode.getBytes());
                    logging.log("[CORE START] Added node to database.", Logging.EntryType.Info);
                }
                // Dispose connection
                conn.disconnect();
            }
            catch(DatabaseException ex)
            {
                logging.log("[CORE START] Failed to check existence of node in database.", ex, Logging.EntryType.Error);
                stop(StopType.Failure);
                return false;
            }
        }
        // Update our IP address and RMI port
        // Initialize the templates manager, load the required templates
        templates = new TemplateManager(this);
        logging.log("[CORE START] Initialized templates.", Logging.EntryType.Info);
        // Load templates from shared folder
        {
            String pathTemplates = Storage.getPath_templates(pathShared);
            File dirTemlates = new File(pathTemplates);
            if(dirTemlates.exists() && dirTemlates.isDirectory())
            {
                if(!templates.loadDir(null, pathTemplates))
                {
                    logging.log("[CORE START] Failed to load shared storage templates at '" + dirTemlates.getPath() + "'!", Logging.EntryType.Error);
                    stop(StopType.Failure);
                    return false;
                }
                else
                    logging.log("[CORE START] Loaded shared storage templates.", Logging.EntryType.Info);
            }
            else
            {
                dirTemlates.mkdir();
                logging.log("[CORE START] Created templates directory in shared file storage.", Logging.EntryType.Info);
            }
        }
        // Initialize web manager
        web = new WebManager(this);
        logging.log("[CORE START] Initialized web manager.", Logging.EntryType.Info);
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
        if(!plugins.reload())
        {
            logging.log("Failed to load plugins.", Logging.EntryType.Error);
            stop(StopType.Failure);
            return false;
        }
        logging.log("[CORE START] Loaded plugins.", Logging.EntryType.Info);
        // Setup comms
        int rmiPort = settings.getInt("rmi/port", 1099);
        try
        {
            comms = new RMI(rmiPort, new RMI_DefaultServer(this));
            if(!comms.start())
                throw new Exception("Could not setup RMI socket.");
        }
        catch(Exception ex)
        {
            logging.log("Failed to setup RMI server.", ex, Logging.EntryType.Error);
            stop(StopType.Failure);
            return false;
        }
        logging.log("[CORE START] Started RMI service on port '" + rmiPort + "'.", Logging.EntryType.Info);
        logging.log("[CORE START] Core started.", Logging.EntryType.Info);
        // Notify any threads
        notifyAll();
        return true;
    }
    /**
     * Stops the core of the node (normal state).
     * @return True = stopped, false = no changes.
     */
    public synchronized boolean stop()
    {
        return stop(StopType.Normal);
    }
    /**
     * Stops the core of the node.
     * @param type The type of core-stop to occur.
     * @return True = stopped, false = no changes.
     */
    public synchronized boolean stop(StopType type)
    {
        if(state != State.Started)
            return false;
        state = State.Stopping;
        logging.log("[CORE STOP] Stopping core...", Logging.EntryType.Info);
        // Notify any threads
        notifyAll();
        // Dispose RMI/comms
        if(comms != null)
        {
            comms.stop();
            comms = null;
        }
        logging.log("[CORE STOP] Disposed RMI...", Logging.EntryType.Info);
        // Unload all the plugins
        if(plugins != null)
        {
            plugins.unload();
            plugins = null;
        }
        logging.log("[CORE STOP] Disposed plugins...", Logging.EntryType.Info);
        // Unload all the templates
        if(templates != null)
        {
            templates.clear();
            templates = null;
        }
        logging.log("[CORE STOP] Disposed templates...", Logging.EntryType.Info);
        // Dispose web-manager
        web = null;
        // Dispose settings
        settings = null;
        // Destroy RNG
        rng = null;
        // Dispose paths
        this.pathPlugins = this.pathShared = null;
        // Decide on new state (and add any appropriate logging)
        State newState;
        switch(type)
        {
            case Failure:
                logging.log("[CORE STOP] Going into failure state...", Logging.EntryType.Info);
                newState = State.Failed;
                break;
            case Shutdown:
                logging.log("[CORE STOP] Going into shutdown state...", Logging.EntryType.Info);
                newState = State.Shutdown;
                break;
            case Normal:
            default:
                newState = State.Stopped;
                break;
        }
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
        logging.log("[CORE STOP] Core stopped.", Logging.EntryType.Info);
        // Notify any threads
        notifyAll();
        return false;
    }
    // Methods *****************************************************************
    /**
     * Creates a new database connector.
     * 
     * @return Instance, else null if the connector could not be made or
     * connect.
     */
    public Connector createConnector()
    {
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
            logging.log("Could not create database connector.", ex, Logging.EntryType.Warning);
        }
        return null;
    }
    // Methods - Waiting Related ***********************************************
    /**
     * Causes the invoking thread to wait until this object is notified; this
     * will occur when the state of the core changes.
     * 
     * @throws InterruptedException Thrown by Object.wait(); refer to
     * third-party documentation.
     */
    public synchronized void waitStateChange() throws InterruptedException
    {
        wait();
    }
    // Methods - Mutators ******************************************************
    /**
     * @param pathPlugins The path of where plugins reside; this will be checked
     * by the plugin-manager when loading all the plugins. If this parameter is
     * incorrect, the plugins will fail to load and the core will fail.
     */
    public synchronized void setPathPlugins(String pathPlugins)
    {
        this.pathPlugins = pathPlugins;
    }
    // Methods - Accessors *****************************************************
    /**
     * Gets the instance of the current PALS node Core.
     * @return 
     */
    public synchronized static NodeCore getInstance()
    {
        if(currentInstance == null)
            currentInstance = new NodeCore();
        return currentInstance;
    }
    /**
     * @return The current state of the core.
     */
    public synchronized State getState()
    {
        return state;
    }
    /**
     * @return The path of where all the plugin libraries reside.
     */
    public String getPathPlugins()
    {
        return pathPlugins;
    }
    /**
     * @return The directory of shared files between node(s) and/or website(s).
     */
    public String getPathShared()
    {
        return pathShared;
    }
    // Methods - Accessors - Components ****************************************
    /**
     * @return The plugin manager responsible for handling the runtime plugins.
     */
    public PluginManager getPlugins()
    {
        return plugins;
    }
    /**
     * @return The template manager responsible for rendering and caching
     * templates.
     */
    public TemplateManager getTemplates()
    {
        return templates;
    }
    /**
     * @return The web manager responsible for handling web-requests.
     */
    public WebManager getWebManager()
    {
        return web;
    }
    /**
     * @return The logger responsible for logging any events with the core.
     */
    public Logging getLogging()
    {
        return logging;
    }
    /**
     * @return Read-only collection of node settings.
     */
    public Settings getSettings()
    {
        return settings;
    }
    /**
     * @return An instance of the Random class, which is created when the core
     * first starts (seeded with the system time).
     */
    public Random getRNG()
    {
        return rng;
    }
    /**
     * @return The UUID used to identify this node.
     */
    public UUID getNodeUUID()
    {
        return uuidNode;
    }
}
