package pals.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

/**
 * Stores base paths of URLs, used by plugins to reserve relative/base URL
 * paths; these are then used to forward web-requests to the plugins.
 * 
 * This means plugins can reserve e.g.:
 * /home
 * 
 * Any requests with the same path, or more sub-directories, are mapped to the
 * same plugin - example:
 * /home
 * /home/a
 * /home/b/c/d/f/etc
 * 
 * If a plugin reserves e.g.:
 * /a/b/
 * 
 * Another plugin can reserve the root directory (/a/) and handle the request,
 * if it fails at /a/b.
 * 
 * This allows for a very flexible URL rewriting system to be implemented
 * between plugins, however the same path cannot be mapped multiple times.
 * 
 * Thread-safe.
 */
public class UrlTree
{
    // Enums *******************************************************************
    /**
     * The status of registering a URL/path.
     */
    public enum RegisterStatus
    {
        Failed_Malformed,
        Failed_AlreadyExists,
        Success
    }
    // Fields ******************************************************************
    private UrlTreeNode root;   // The root node of the tree.
    // Methods - Constructors **************************************************
    /**
     * Creates a new empty URL tree.
     */
    public UrlTree()
    {
        reset();
    }
    // Methods *****************************************************************
    private synchronized String[] createParts(String path)
    {
        // Remove whitespace
        path = path.trim();
        // Remove tailing slashes
        if(path.startsWith("/"))
        {
            if(path.length() == 1)
                return null;
            path = path.substring(1);
        }
        if(path.endsWith("/"))
        {
            if(path.length() == 1)
                return null;
            path = path.substring(0, path.length());
        }
        // Check the path is not empty
        if(path.length() == 0)
            return null;
        // Explode and return array - this could do with further validation against e.g. RFC 3986, but this will do for now.
        return path.split("/");
    }
    // Methods - Mutators ******************************************************
    /**
     * Resets the URL tree.
     */
    public synchronized void reset()
    {
        // Note: terminator must be false due to methods such as getUuids
        // using the terminator indicator to add paths
        this.root = new UrlTreeNode(null, false);
    }
    /**
     * Adds a new path to the URL tree.
     * 
     * @param plugin The owner of the path.
     * @param path The path of the URL; without tailing forward slashes.
     * @return The status of attempting to add the URL.
     */
    public synchronized RegisterStatus add(Plugin plugin, String path)
    {
        if(plugin == null)
            return RegisterStatus.Failed_Malformed;
        // Either return malformed (null parts) or recurse parts and add them...
        String[] parts = createParts(path);
        return parts == null ? RegisterStatus.Failed_Malformed : add(plugin.getUUID(), root, 0, parts);
    }
    private synchronized RegisterStatus add(UUID uuid, UrlTreeNode currNode, int pathOffset, String[] pathParts)
    {
        boolean terminator = pathOffset+1 >= pathParts.length;      // Indicates if the current URL part should be a terminator
        
        // Check if any more parts are needed - else we have succeeded!
        if(pathOffset >= pathParts.length)
            return RegisterStatus.Success;
        // Fetch the next part; if it exists, return failure - else create the node
        UrlTreeNode nextChild = currNode.get(pathParts[pathOffset]);
        if(nextChild != null && terminator)
        {
            if(nextChild.isTerminator())    // Last part + exists -> path already exists
                return RegisterStatus.Failed_AlreadyExists;
            else
            {
                nextChild.setUuid(uuid);
                nextChild.setTerminator(true);
                return RegisterStatus.Success;
            }
        }
        else if(nextChild == null)                                  // New path - add to current node
        {
            nextChild = new UrlTreeNode(terminator ? uuid : null, terminator);
            currNode.childAdd(pathParts[pathOffset], nextChild);
        }
        // Recurse new node to add further parts
        return add(uuid, nextChild, ++pathOffset, pathParts);
    }
    /**
     * Removes all of the paths associated with a plugin.
     * 
     * @param plugin The plugin which owns the path(s).
     */
    public synchronized void remove(Plugin plugin)
    {
        // Iterate each node recursively
        root.purge(plugin.getUUID());
    }
    // Methods - Accessors *****************************************************
    /**
     * Fetches all the plugins associated with a path. Since a path can
     * have multiple directories, e.g. path/a/b/c, each directory can be
     * owned by a different plugin. The order of UUIDs returned starts from
     * the highest directory, towards the root. UUIDs are also unique.
     * 
     * @param path The path of which to fetch plugins.
     * @return Array of plugins for the specified path.
     */
    public UUID[] getUUIDs(String path)
    {
        ArrayList<UUID> result = new ArrayList<>();
        // Explode into parts
        String[] parts = createParts(path);
        if(parts == null)           // Invalid path...
            return new UUID[0];
        // Iterate tree for best matches
        UrlTreeNode curr = root;
        int partOffset = 0;
        UUID uuid;
        do
        {
            curr = curr.get(parts[partOffset]);
            if(curr != null && curr.isTerminator())
            {
                uuid = curr.getUuid();
                if(uuid != null && !result.contains(uuid))
                    result.add(uuid);
            }
        }
        while(curr != null && ++partOffset < parts.length);
        // Reverse the list - the most likely candidates are those further down the tree
        Collections.reverse(result);
        return result.toArray(new UUID[result.size()]);
    }

    // Methods - Debug *********************************************************
    /**
     * @return All the URLs currently held by the collection.
     */
    public synchronized String[] getUrls()
    {
        ArrayList<String> result = new ArrayList<>();
        // Recursively iterate and build tree of URLs
        getUrls(result, root, "");
        return result.toArray(new String[result.size()]);
    }
    private void getUrls(ArrayList<String> result, UrlTreeNode node, String currentUrl)
    {
        // Check the current node
        if(node.isTerminator())
            result.add(currentUrl.substring(0, currentUrl.length()));
        // Recurse children
        for(Map.Entry<String,UrlTreeNode> cn : node.children.entrySet())
            getUrls(result, cn.getValue(), currentUrl + "/" + cn.getKey());
    }
}
