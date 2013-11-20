package pals.base;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import pals.base.utils.Files;
import pals.base.web.WebRequestData;

/**
 * Used for caching templates from the file-system and registering template
 * functions for rendering.
 * 
 * Thread-safe.
 */
public class TemplateManager
{
    // Fields ******************************************************************
    private final NodeCore                          core;       // The current instance of the core.
    private final HashMap<String, TemplateFunction> functions;  // The template functions used for rendering; function-name,function.
    private final HashMap<String, Template>         templates;  // Cached templates; path,template.
    // Methods - Constructors **************************************************    
    protected TemplateManager(NodeCore core)
    {
        this.core = core;
        this.functions = new HashMap<>();
        this.templates = new HashMap<>();
    }
    // Methods *****************************************************************
    /**
     * Reloads all the templates by calling every active plugin to re-register
     * all their functions and templates.
     * @return True if successful, false if failed.
     */
    public synchronized boolean reload()
    {
        core.getLogging().log("Re-registering all templates...", Logging.EntryType.Info);
        Plugin[] plugins = core.getPlugins().getPlugins();
        for(Plugin plugin : plugins)
        {
            if(!plugin.eventHandler_registerTemplates(core, this))
            {
                core.getLogging().log("Failed to register templates for plugin '" + plugin.getTitle() + "' (" + plugin.getUUID().getHexHyphens() + ").", Logging.EntryType.Warning);
                return false;
            }
        }
        core.getLogging().log("Finished re-registering all templates.", Logging.EntryType.Info);
        return true;
    }
    /**
     * Loads templates from a physical directory. Inside each file should be
     * the content of the template. The file-name is concatanated with the
     * name(s) of any sub-directories. If you were to specify 'C:/test' and a
     * template was found at 'C:/test/hello/world.template', the name/path would
     * be 'hello/world'.
     * 
     * @param plugin The plugin which owns the templates.
     * @param directory The path of the directory.
     * @param pathPrefix The prefix to append to the files loaded.
     * @return True = success, false = failed.
     */
    public synchronized boolean load(Plugin plugin, String directory, String pathPrefix)
    {
        try
        {
            // Iterate each file and cache the contents
            File[] files = Files.getAllFiles(directory, true, false, ".template", true);
            String content, path;
            for(File f : files)
            {
                content = Files.fileRead(f.getPath());
                path = f.getPath().substring(pathPrefix.length());
                // Attempt to register
                if(!registerTemplate(plugin, path, content))
                {
                    core.getLogging().log("Failed to register template '" + path + "'; aborted loading dir '" + directory + "'!", Logging.EntryType.Warning);
                    return false;
                }
            }
            return true;
        }
        catch(FileNotFoundException ex)
        {
            core.getLogging().log("Failed to register templates at '" + directory + "' ~ FieNotFound ~ ", ex, Logging.EntryType.Warning);
        }
        catch(IOException ex)
        {
            core.getLogging().log("Failed to register templates at '" + directory + "' ~ IOException ~ ", ex, Logging.EntryType.Warning);
        }
        return false;
    }
    /**
     * Unloads all the templates and registered template functions.
     */
    public synchronized void unload()
    {
        templates.clear();
        functions.clear();
    }
    /**
     * Unloads all the templates and registered template functions belonging to
     * a plugin.
     * 
     * @param plugin The identifier of the plugin.
     */
    public synchronized void unload(UUID plugin)
    {
        Iterator<Map.Entry<String,Template>> it = templates.entrySet().iterator();
        Map.Entry<String,Template> template;
        while(it.hasNext())
        {
            template = it.next();
            if(template.getValue().getPluginOwner().equals(plugin))
                it.remove();
        }
    }
    /**
     * Registers a template function.
     * 
     * @param functionName The name of the function; can be alpha-numeric,
     * contain hyphens, under-scrolls, forward slashes and dot.
     * @param function
     * @return True if registered, false if failed (invalid format, path
     * already taken).
     */
    public synchronized boolean registerFunction(String functionName, TemplateFunction function)
    {
        // Check the function format is correct and does not already exist
        if(!functionName.matches("^([a-zA-Z0-9\\.\\_\\-\\/]+)$") || functions.containsKey(functionName))
            return false;
        // Register
        functions.put(functionName, function);
        return true;
    }
    /**
     * Registers a template.
     * 
     * @param plugin The plugin which owns the template; only the UUID is stored.
     * @param path The path of the template; can contain alpha-numeric chars,
     * hyphen,underscroll, dot and forward-slash.
     * @param content The content of the template.
     * @return True if registered, false if failed (invalid format, path
     * already taken).
     */
    public synchronized boolean registerTemplate(Plugin plugin, String path, String content)
    {
        // Check the template doesn't exist
        if(!path.contains("^([a-zA-Z0-9\\.\\_\\-\\/]+)$") || templates.containsKey(path))
            return false;
        // Create wrapper and cache
        templates.put(path, new Template(path, content, plugin.getUUID()));
        return true;
    }
    /**
     * Renders content using template functions.
     * 
     * @param data The data from a web-request.
     * @param content The content of the template.
     * @return The rendered data.
     */
    public synchronized String render(WebRequestData data, String content)
    {
        return null;
    }
    // Methods - Accessors *****************************************************
    /**
     * Fetches the content of a template from the collection.
     * 
     * @param path The path of the template node.
     * @return Template content or null.
     */
    public synchronized String getTemplate(String path)
    {
        return getTemplate(path, null);
    }
    /**
     * Fetches the content of a template from the collection.
     * 
     * @param path The path of the template node.
     * @param alternative If the node is not found, this is returned instead.
     * @return Template content or the alternative parameter.
     */
    public synchronized String getTemplate(String path, String alternative)
    {
        Template node = getTemplateNode(path);
        return node == null ? alternative : node.getContent();
    }
    /**
     * Fetches a node from the collection.
     * 
     * @param path The path of the template node.
     * @return Node or null.
     */
    public synchronized Template getTemplateNode(String path)
    {
        return templates.get(path);
    }
}
