package pals.base;

/**
 * A model for holding RMI information for a node.
 */
public class RMI_Host
{
    // Fields ******************************************************************
    private UUID    uuid;
    private String  host;
    private int     port;
    // Methods - Constructors **************************************************
    public RMI_Host(UUID uuid, String host, int port)
    {
        this.uuid = uuid;
        this.host = host;
        this.port = port;
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The UUID of the node.
     */
    public UUID getUUID()
    {
        return uuid;
    }
    /**
     * @return The IP of the host.
     */
    public String getHost()
    {
        return host;
    }
    /**
     * @return The port of the RMI registry of the host.
     */
    public int getPort()
    {
        return port;
    }
}
