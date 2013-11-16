package pals.base;

/**
 * A template cached by the template manager.
 */
public class Template
{
    // Fields ******************************************************************
    private final String    path;       // The path of the template.
    private final String    content;    // The content of the template.
    private final UUID      plugin;     // The plugin which owns the template; can be null.
    // Methods - Constructors **************************************************
    protected Template(String path, String content, UUID plugin)
    {
        this.path = path;
        this.content = content;
        this.plugin = plugin;
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The path of the template.
     */
    public String getPath()
    {
        return path;
    }
    /**
     * @return The content of the template.
     */
    public String getContent()
    {
        return content;
    }
    /**
     * @return The plugin which owns the template; can be null.
     */
    public UUID getPluginOwner()
    {
        return plugin;
    }
}
