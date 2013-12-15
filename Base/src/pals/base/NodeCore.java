package pals.base;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
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
        Failed
    }
    // Constants ***************************************************************
    private static final String defaultPathPlugins = "_plugins";    // The default path of where plugins reside.
    // Fields - Instance *******************************************************
    private static NodeCore     currentInstance = null;             // The current instance of the NodeCore.
    // Fields ******************************************************************
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
        this.pathPlugins = defaultPathPlugins;
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
        // Initialize RNG
        rng = new Random(System.currentTimeMillis());
        // Start logging
        logging = Logging.createInstance("system", true);
        if(logging == null)
        {
            System.err.println("Failed to start core logging, aborted!");
            stop(true);
            return false;
        }
        logging.log("[CORE START] Started logging.", Logging.EntryType.Info);
        // Load base node settings
        try
        {
            settings = Settings.load("_config/node.config", true);
        }
        catch(SettingsException ex)
        {
            logging.log("Failed to node.config settings - '" + ex.getExceptionType().toString() + "' ~ '" + ex.getMessage() + "'.", Logging.EntryType.Error);
            stop(true);
            return false;
        }
        logging.log("[CORE START] Loaded settings.", Logging.EntryType.Info);
        // Check the storage path exists and we have read/write permissions
        {
            pathShared = settings.getStr("storage/path");
            // Validate setting
            if(pathShared == null || pathShared.length() == 0)
            {
                logging.log("[CORE START] Setting 'storage/path' is missing or invalid!", Logging.EntryType.Error);
                stop(true);
                return false;
            }
            // Validate access to path
            Storage.StorageAccess access = Storage.checkAccess(pathShared, true, true, true, true);
            switch(access)
            {
                case DoesNotExist:
                    logging.log("[CORE START] Setting 'storage/path', with value '" + pathShared + "': path does not exist!", Logging.EntryType.Error);
                    stop(true);
                    return false;
                case CannotRead:
                    logging.log("[CORE START] Setting 'storage/path', with value '" + pathShared + "': no read permissions!", Logging.EntryType.Error);
                    stop(true);
                    return false;
                case CannotWrite:
                    logging.log("[CORE START] Setting 'storage/path', with value '" + pathShared + "': no write permissions!", Logging.EntryType.Error);
                    stop(true);
                    return false;
                case CannotExecute:
                    logging.log("[CORE START] Setting 'storage/path', with value '" + pathShared + "': no execute permissions!", Logging.EntryType.Error);
                    stop(true);
                    return false;
            }
            // Log the storage path
            File storage = new File(pathShared);
            try
            {
                logging.log("[CORE START] Storage path '" + storage.getCanonicalPath() + "' checked, with r+w+e permissions.", Logging.EntryType.Info);
            }
            catch(IOException ex)
            {
                logging.log("[CORE START] Storage path '" + storage.getPath() + "' (#2) checked, with r+w+e permissions.", Logging.EntryType.Info);
            }
        }
        // Create an initial connection to the database
        Connector conn = createConnector();
        if(conn == null)
        {
            System.err.println("Failed to create database connector.");
            logging.log("Failed to create database connector.", Logging.EntryType.Error);
            stop(true);
            return false;
        }
        logging.log("[CORE START] Established database connection.", Logging.EntryType.Info);
        // Initialize the templates manager, load the required templates
        templates = new TemplateManager(this);
        logging.log("[CORE START] Initialized templates.", Logging.EntryType.Info);
        // Initialize web manager
        web = new WebManager(this);
        logging.log("[CORE START] Initialized web manager.", Logging.EntryType.Info);
        // Initialize plugin manager, load all the plugins
        plugins = new PluginManager(this);
        // -- Attempt to load the plugins path from settings, if it exists
        try
        {
            setPathPlugins((String)settings.get2("plugins/path"));
        }
        catch(SettingsException ex)
        {
            // Does not exist - ignore and use default internal setting!
        }
        if(!plugins.load())
        {
            logging.log("Failed to load plugins.", Logging.EntryType.Error);
            stop(true);
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
            stop(true);
            return false;
        }
        logging.log("[CORE START] Started RMI service on port '" + rmiPort + "'.", Logging.EntryType.Info);
        logging.log("[CORE START] Core started.", Logging.EntryType.Info);
        return true;
    }
    /**
     * Stops the core of the node.
     * @return True = stopped, false = no changes.
     */
    public synchronized boolean stop()
    {
        return stop(false);
    }
    /**
     * Stops the core of the node.
     * @param failure Indicates the core is to stop due to a failure.
     * @return True = stopped, false = no changes.
     */
    public synchronized boolean stop(boolean failure)
    {
        if(state != State.Started)
            return false;
        state = State.Stopping;
        logging.log("[CORE STOP] Stopping core...", Logging.EntryType.Info);
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
            templates.unload();
            templates = null;
        }
        logging.log("[CORE STOP] Disposed templates...", Logging.EntryType.Info);
        // Dispose web-manager
        web = null;
        // Dispose settings
        settings = null;
        // Unload logging
        if(logging != null)
        {
            logging.dispose(); // This should always be last!
            logging = null;
        }
        logging.log("[CORE STOP] Disposed logging...", Logging.EntryType.Info);
        // Destroy RNG
        rng = null;
        // Update state
        state = failure ? State.Failed : State.Stopped;
        logging.log("[CORE STOP] Core stopped.", Logging.EntryType.Info);
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
}
