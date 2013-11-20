package pals.base;

/**
 * Responsible for managing requests forwarded to plugins.
 * 
 * Thread-safe.
 */
public class WebManager
{
    // Fields ******************************************************************
    private NodeCore    core;           // The current instance of the node core.
    private UrlTree     urls;           // Used for finding which plugins are used when forwarding requests.
    // Methods - Constructors **************************************************
    protected WebManager(NodeCore core)
    {
        this.core = core;
    }
    // Methods
    
    // Methods - Accessors
}
