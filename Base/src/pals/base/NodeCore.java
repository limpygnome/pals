package pals.base;

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
    // Fields - Instance *******************************************************
    private static NodeCore currentInstance = null;     // The current instance of the NodeCore.
    // Fields ******************************************************************
    private State           state;                      // The current state of the core.
    // Fields - Core Components ************************************************
    private PluginManager   plugins;                    // A collection of plugins for the system.
    private TemplateManager templates;                  // A collection of templates for the system.
    private Logging         logging;                    // Logging of system events.
    private Settings        settings;                   // Read-only core settings loaded from file.
    // Methods - Constructors **************************************************
    public NodeCore()
    {
        this.state = State.Stopped;
        this.plugins = null;
        this.templates = null;
        this.logging = null;
        this.settings = null;
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
            state = State.Failed;
            return false;
        }
        // Load base node settings
        try
        {
            settings = Settings.load("node.config", true);
        }
        catch(SettingsException ex)
        {
            System.err.println("Failed to node.config settings - '" + ex.getExceptionType().toString() + "' ~ '" + ex.getMessage() + "'.");
            state = State.Failed;
            return false;
        }
        // Create an initial connection to the database
        Connector conn = createConnector();
        if(conn == null)
        {
            System.err.println("Failed to create database connector.");
            state = State.Failed;
            return false;
        }
        // Initialize the templates manager, load the required templates
        
        // Initialize plugin manager, load all the plugins
        
        return false;
    }
    public synchronized boolean stop()
    {
        state = State.Stopping;
        // Unload all the plugins
        
        // Dispose base components
        // -- plugins
        // -- templates
        settings = null;
        logging.dispose(); // This should always be last!
        logging = null;
        // Update state
        state = State.Stopped;
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
