package pals.base;

/**
 * A class used for an instance of a node; this is responsible for node features
 * such as plugin management, logging and node intercommunication.
 * 
 * Only one NodeCore may run; singleton design pattern enforced.
 */
public class NodeCore
{
    // Fields - Instance *******************************************************
    private static NodeCore currentInstance = null;     // The current instance of the NodeCore.
    // Fields - Core Components ************************************************
    private PluginManager   plugins;                    // A collection of plugins for the system.
    private PALSP           comms;                      // Used for inter-node communication.
    private Logging         logging;                    // Logging of system events.
    private Settings        settingsCore;               // Read-only core settings loaded from file.
    private Settings        settings;                   // Settings stored in the database, used for plugins.
    // Methods - Constructors **************************************************
    public NodeCore()
    {
        this.plugins = null;
        this.comms = null;
        this.logging = null;
        this.settingsCore = null;
        this.settings = null;
    }
    // Methods - Core State ****************************************************
    public boolean start()
    {
        return false;
    }
    public boolean stop()
    {
        return false;
    }
    // Methods - Accessors *****************************************************
    /**
     * Gets the instance of the current PALS node Core.
     * @return 
     */
    public static NodeCore getInstance()
    {
        if(currentInstance == null)
            currentInstance = new NodeCore();
        return currentInstance;
    }   
}
