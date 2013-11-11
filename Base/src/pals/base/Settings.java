package pals.base;

import java.util.HashMap;

/**
 * A collection which holds key/value settings data.
 */
public class Settings
{
    // Fields ******************************************************************
    private boolean readOnly;                           // Indicates if the collection is read-only.
    private HashMap<String,SettingsNode> settings;      // Path,node with settings data.
    // Methods - Constructors **************************************************
    
    // Methods - Persistence ***************************************************
    
    // Methods - Accessors *****************************************************
    public boolean isReadOnly()
    {
        return readOnly;
    }
    /**
     * Fetches a settings node.
     * @param path The path of the node.
     * @return Node data or null.
     */
    public SettingsNode getNode(String path)
    {
        return settings.get(path);
    }
    /**
     * Fetches the data of a node.
     * @param path The path of the node.
     * @return The data of the node as a String.
     */
    public String get(String path)
    {
    }
}
