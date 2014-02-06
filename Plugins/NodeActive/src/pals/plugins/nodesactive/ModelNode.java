package pals.plugins.nodesactive;

import java.util.ArrayList;
import org.joda.time.DateTime;
import org.joda.time.Period;
import pals.base.Logging;
import pals.base.NodeCore;
import pals.base.UUID;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.database.Result;
import pals.base.utils.Misc;

public class ModelNode
{
    // Fields ******************************************************************
    private UUID        uuid;
    private String      title;
    private DateTime    lastActive;
    private String      rmiIP;
    private Integer     rmiPort;
    // Methods - Constructors **************************************************
    private ModelNode(UUID uuid, String title, DateTime lastActive, String rmiIP, Integer rmiPort)
    {
        this.uuid = uuid;
        this.title = title;
        this.lastActive = lastActive;
        this.rmiIP = rmiIP;
        this.rmiPort = rmiPort;
    }
    // Methods - Persistence ***************************************************
    /**
     * @param conn Database connector.
     * @param uuidNode The UUID of the node.
     * @return An instance of a model or null.
     */
    public static ModelNode load(Connector conn, UUID uuidNode)
    {
        try
        {
            Result res = conn.read("SELECT * FROM pals_nodes WHERE uuid_node=?;", uuidNode.getBytes());
            return res.next() ? load(res) : null;
        }
        catch(DatabaseException ex)
        {
            return null;
        }
    }
    /**
     * Loads all of the available nodes.
     * 
     * @param conn Database connector.
     * @return Array of nodes; can be empty.
     */
    public static ModelNode[] load(Connector conn)
    {
        try
        {
            ArrayList<ModelNode> buffer = new ArrayList<>();
            Result res = conn.read("SELECT * FROM pals_nodes ORDER BY title ASC");
            ModelNode node;
            while(res.next())
            {
                if((node = load(res)) != null)
                    buffer.add(node);
            }
            return buffer.toArray(new ModelNode[buffer.size()]);
        }
        catch(DatabaseException ex)
        {
            return new ModelNode[0];
        }
    }
    /**
     * Loads a model from a result.
     * 
     * @param res The result with the data; the method next should be invoked.
     * @return An instance of the model or null.
     */
    public static ModelNode load(Result res)
    {
        try
        {
            Object rmiIP = res.get("rmi_ip");
            Object rmiPort = res.get("rmi_port");
            if(rmiIP == null || rmiPort == null)
                rmiIP = rmiPort = null;
            return new ModelNode(UUID.parse((byte[])res.get("uuid_node")), (String)res.get("title"), new DateTime(res.get("last_active")), rmiIP != null ? (String)rmiIP : null, rmiPort != null ? (Integer)rmiPort : null);
        }
        catch(DatabaseException ex)
        {
            NodeCore.getInstance().getLogging().logEx("test", ex, Logging.EntryType.Info);
            return null;
        }
    }

    // Methods - Accessors *****************************************************
    /**
     * @return The UUID used to identify the node.
     */
    public UUID getUUID()
    {
        return uuid;
    }
    /**
     * @return The title of the node.
     */
    public String getTitle()
    {
        return title;
    }
    /**
     * @return The date and time the node was last active.
     */
    public DateTime getLastActive()
    {
        return lastActive;
    }
    /**
     * @return Human-representation of last-active date.
     */
    public String getLastActiveHuman()
    {
        return Misc.humanDateTime(lastActive);
    }
    /**
     * @return Indicates if the node is active.
     */
    public boolean isOnline()
    {
        return (System.currentTimeMillis()-lastActive.toDate().getTime()) < 60000;
    }
    /**
     * @return The IP of the node for RMI; can be null.
     */
    public String getRmiIP()
    {
        return rmiIP;
    }
    /**
     * @return The port of the node for RMI; can be null.
     */
    public Integer getRmiPort()
    {
        return rmiPort;
    }
}
