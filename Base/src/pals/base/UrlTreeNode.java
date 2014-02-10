package pals.base;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A node in a Url Tree data-structure.
 * 
 * Thread-safe.
 */
class UrlTreeNode
{
    // Fields ******************************************************************
    private boolean                     terminator;                     // Indicates the node is a terminator.
    private UUID                        plugin;                         // The plugin which owns this node/url-path.
    final HashMap<String, UrlTreeNode>  children = new HashMap<>();     // Map of child paths to their tree nodes.
    // Methods - Constructors **************************************************
    UrlTreeNode(UUID plugin, boolean terminator)
    {
        this.plugin = plugin;
        this.terminator = terminator;
    }
    // Methods - Mutators ******************************************************
    /**
     * Adds a new child node.
     * 
     * @param dir The directory of the child.
     * @param node The node of the child.
     * @return True = added, false = already exists.
     */
    synchronized boolean childAdd(String dir, UrlTreeNode node)
    {
        if(children.containsKey(dir))
            return false;
        children.put(dir, node);
        return true;
    }
    /**
     * Removes a child node.
     * 
     * @param dir The directory of the child.
     * @return True = removed, false = not found.
     */
    synchronized boolean childRemove(String dir)
    {
        return children.remove(dir) != null;
    }
    synchronized void purge(UUID uuid)
    {
        // Remove UUID recursively
        UrlTreeNode node;
        UUID nodeUuid;
        Iterator<Map.Entry<String,UrlTreeNode>> it = children.entrySet().iterator();
        while(it.hasNext())
        {
            node = it.next().getValue();
            // Inform the node to perform a purge on its children
            node.purge(uuid);
            // If it has children, only set the UUID to null / terminator to false etc
            nodeUuid = node.getUuid();
            if(node.children.size() > 0)
            {
                if(nodeUuid != null && nodeUuid.equals(uuid))
                {
                    node.setTerminator(false);
                    node.setUuid(null);
                }
            }
            // No children? Remove the node - it has no dependencies!
            else if(nodeUuid != null && nodeUuid.equals(uuid))
                it.remove();
        }
    }
    void setTerminator(boolean terminator)
    {
        this.terminator = terminator;
    }
    void setUuid(UUID plugin)
    {
        this.plugin = plugin;
    }
    // Methods - Accessors *****************************************************
    boolean isTerminator()
    {
        return terminator;
    }
    UUID getUuid()
    {
        return plugin;
    }
    synchronized UrlTreeNode get(String directory)
    {
        return children.get(directory);
    }
}
