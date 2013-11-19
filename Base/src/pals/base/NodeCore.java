package pals.base;

import java.rmi.RemoteException;
import pals.base.database.Connector;
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
    // Fields - Core Components ************************************************
    private PluginManager       plugins;                            // A collection of plugins for the system.
    private TemplateManager     templates;                          // A collection of templates for the system.
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
        this.logging = null;
        this.settings = null;
        this.comms = null;
    }
    // Methods - Core **********************************************************
    public synchronized boolean start()
    {
        // Check the core is in either the stopped or failed state to start
        if(state != State.Failed && state != State.Stopped)
            return false;
        // Update the state to starting...
        state = State.Starting;
        // Start logging
        logging = Logging.createInstance("system", true);
        if(logging == null)
        {
            System.err.println("Failed to start core logging, aborted!");
            stop(true);
            return false;
        }
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
        // Create an initial connection to the database
        Connector conn = createConnector();
        if(conn == null)
        {
            System.err.println("Failed to create database connector.");
            logging.log("Failed to create database connector.", Logging.EntryType.Error);
            stop(true);
            return false;
        }
        // Initialize plugin manager, load all the plugins
        plugins = new PluginManager(this);
        // -- Attempt to load the plugins path from settings, if it exists
        try
        {
            setPathPlugins((String)settings.get2("plugins/path"));
        }
        catch(SettingsException ex)
        {
            // Doesn't exist - ignore and use default internal setting!
        }
        if(!plugins.load())
        {
            logging.log("Failed to load plugins.", Logging.EntryType.Error);
            stop(true);
            return false;
        }
        // Initialize the templates manager, load the required templates
        templates = new TemplateManager(this);
        if(!templates.reload())
        {
            logging.log("Failed to load templates.", Logging.EntryType.Error);
            stop(true);
            return false;
        }
        // Setup comms
        try
        {
            comms = new RMI(settings.getInt("rmi/port", 1099), new RMI_DefaultServer());
        }
        catch(RemoteException ex)
        {
            logging.log("Failed to setup RMI server.", ex, Logging.EntryType.Error);
            stop(true);
            return false;
        }
        return false;
    }
    public synchronized boolean stop()
    {
        return stop(false);
    }
    public synchronized boolean stop(boolean failure)
    {
        state = State.Stopping;
        // Dispose RMI/comms
        if(comms != null)
        {
            comms.stop();
            comms = null;
        }
        // Unload all the plugins
        if(plugins != null)
        {
            plugins.unload();
            plugins = null;
        }
        // Unload all the templates
        if(templates != null)
        {
            templates.unload();
            templates = null;
        }
        // Dispose settings
        settings = null;
        // Unload logging
        if(logging != null)
        {
            logging.dispose(); // This should always be last!
            logging = null;
        }
        // Update state
        state = failure ? State.Failed : State.Stopped;
        return false;
    }
    // Methods *****************************************************************
    /**
     * @return Creates a new database connector.
     */
    public Connector createConnector()
    {
        Connector conn = null;
        switch(settings.getInt("database/type"))
        {
            case Postgres.IDENTIFIER_TYPE:
                conn = new Postgres(settings.getStr("database/host"), settings.getStr("database/db"), settings.getStr("database/username"), settings.getStr("database/password"), settings.getInt("database/port"));
                break;
            case MySQL.IDENTIFIER_TYPE:
                conn = new MySQL(settings.getStr("database/host"), settings.getStr("database/db"), settings.getStr("database/username"), settings.getStr("database/password"), settings.getInt("database/port"));
                break;
        }
        return conn;
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
    public String getPathPlugins()
    {
        return pathPlugins;
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
