package pals.base;

/**
 * Used to store information about a template.
 */
public class TemplateItem
{
    // Fields ******************************************************************
    private final UUID      plugin;         // The plugin which owns the template.
    private final String    path;           // The path of the template.
    private final String    data;           // The data of the template.
    private final long      lastModified;   // The time at which the template was modified; used by FreeMarker exclusively.
    // Methods - Constructors **************************************************
    public TemplateItem(UUID plugin, String path, String data)
    {
        this.plugin = plugin;
        this.path = path;
        this.data = data;
        this.lastModified = System.currentTimeMillis();
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The plugin which owns the template; can be null.
     */
    public UUID getPluginUUID()
    {
        return plugin;
    }
    /**
     * @return The path of the template.
     */
    public String getPath()
    {
        return path;
    }
    /**
     * @return The data of the template.
     */
    public String getData()
    {
        return data;
    }
    /**
     * @return The time at which the template was modified; used by FreeMarker
     * exclusively.
     */
    public long getLastModified()
    {
        return lastModified;
    }
}
