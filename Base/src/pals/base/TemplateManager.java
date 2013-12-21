package pals.base;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import pals.base.utils.Files;
import pals.base.utils.JarIO;
import pals.base.utils.JarIOException;
import pals.base.web.WebRequestData;

/**
 * Used to manage (cache/process/interface) templates; these are primarily for
 * rendering HTML for web-pages, however they can be multi-purpose for e.g.
 * e-mail messages.
 * 
 * Thread-safe.
 */
public class TemplateManager
{
    // Fields ******************************************************************
    private final NodeCore                          core;               // The current instance of the core.
    private final HashMap<String, TemplateFunction> functions;          // The template functions used for rendering; function-name,function.
    // Fields - Regex **********************************************************
    private final Pattern                           pattFunctions;      // Used to match function calls.
    // Fields - FreeMarker Template Engine *************************************
    private TemplateLoader                          fmLoader;           // Used to load and cache templates.
    private Configuration                           fmConfig;           // General configuration.
    // Methods - Constructors **************************************************    
    protected TemplateManager(NodeCore core)
    {
        this.core = core;
        this.functions = new HashMap<>();
        // Compile regex pattern(s)
        pattFunctions = Pattern.compile("<!--([a-zA-Z0-9_]+)\\(([^\\(\\)]*)\\)-->");
        // Setup FreeMarker template engine
        setupFreeMarker();
    }
    // Methods *****************************************************************
    private void setupFreeMarker()
    {
        fmLoader = new TemplateLoader();            // Used for loading, storing and interfacing with templates.
        fmConfig = new Configuration();             // Used for configuration and caching (http://freemarker.org/docs/pgui_config_templateloading.html) templates.
        fmConfig.setTemplateLoader(fmLoader);       // Sets the loader to use.
        fmConfig.setDefaultEncoding("UTF-8");       // Sets the encoding of templates.
        fmConfig.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);     // Rethrows exceptions; recommended in production systems (http://freemarker.org/docs/api/freemarker/template/TemplateExceptionHandler.html#RETHROW_HANDLER).
    }
    /**
     * Reloads all the templates by calling every active plugin to re-register
     * all their functions and templates.
     * @return True if successful, false if failed.
     */
    public synchronized boolean reload()
    {
        core.getLogging().log("[TEMPLATES] Re-registering all templates...", Logging.EntryType.Info);
        // Clear templates
        fmLoader.clear();
        // Reset template cache
        fmConfig.clearTemplateCache();
        // Invoke all plugins to re-register templates
        Plugin[] plugins = core.getPlugins().getPlugins();
        for(Plugin plugin : plugins)
        {
            if(!plugin.eventHandler_registerTemplates(core, this))
            {
                core.getLogging().log("[TEMPLATES] Failed to register templates for plugin '" + plugin.getTitle() + "' [" + plugin.getUUID().getHexHyphens() + "].", Logging.EntryType.Warning);
                return false;
            }
        }
        core.getLogging().log("[TEMPLATES] Finished re-registering all templates.", Logging.EntryType.Info);
        return true;
    }
    /**
     * Loads a template from file.
     * 
     * @param plugin The plugin which owns the template.
     * @param physicalPath The physical path of the template.
     * @param path
     * @return 
     */
    public synchronized boolean loadFile(Plugin plugin, String physicalPath, String path)
    {
        try
        {
            registerTemplate(plugin, path, Files.fileRead(physicalPath));
            return true;
        }
        catch(IOException ex)
        {
            core.getLogging().log("[TEMPLATES] Failed to load template at '" + path + "' ~ plugin [" + (plugin != null ? plugin.getUUID().getHexHyphens() : "unknown") + "]!", ex, Logging.EntryType.Warning);
            return false;
        }
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
     * @return True = success, false = failed.
     */
    public synchronized boolean loadDir(Plugin plugin, String directory)
    {
        try
        {
            // Iterate each file and cache the contents
            File[] files = Files.getAllFiles(directory, false, true, ".template", true);
            String content, fpath, path;
            for(File f : files)
            {
                fpath = f.getPath();
                content = Files.fileRead(fpath);
                path = fpath.substring(directory.length()+1,fpath.length()-9); // -9 = .template length
                // Attempt to register
                if(!registerTemplate(plugin, path, content))
                {
                    core.getLogging().log("[TEMPLATES] Failed to register template '" + path + "' ~ plugin [" + (plugin != null ? plugin.getUUID().getHexHyphens() : "unknown") + "]; aborted loading dir '" + directory + "'!", Logging.EntryType.Warning);
                    return false;
                }
            }
            return true;
        }
        catch(FileNotFoundException ex)
        {
            core.getLogging().log("[TEMPLATES] Failed to register templates at '" + directory + "' ~ plugin [" + (plugin != null ? plugin.getUUID().getHexHyphens() : "unknown") + "] ~ FieNotFound ~ ", ex, Logging.EntryType.Warning);
        }
        catch(IOException ex)
        {
            core.getLogging().log("[TEMPLATES] Failed to register templates at '" + directory + "' ~ plugin [" + (plugin != null ? plugin.getUUID().getHexHyphens() : "unknown") + "] ~ IOException ~ ", ex, Logging.EntryType.Warning);
        }
        return false;
    }
    /**
     * Loads templates from a Java Archive (JAR). These templates should end
     * with the extension '.template'. Sub-directories are included. The names
     * of templates are:
     * 
     *  offset path (from the directory specified)/filename(excluding extension)
     * 
     * Note: this creates an instance of JarIO to read the archive; if JarIO
     * is already open, the method load(Plugin, JarIO, String) should be used
     * instead.
     * 
     * @param plugin The plugin which owns the JAR.
     * @param directory The directory of where the templates exist. This is the
     * name of the package, but with the dots replaced by forward-slash. This
     * can be null/empty for the entire JAR.
     * @return True = loaded, false = could not load.
     */
    public synchronized boolean load(Plugin plugin, String directory)
    {
        return load(plugin, plugin.getJarIO(), directory);
    }
    /**
     * Loads templates from a Java Archive (JAR). These templates should end
     * with the extension '.template'. Sub-directories are included. The names
     * of templates are:
     * 
     *  offset path (from the directory specified)/filename(excluding extension)
     * 
     * @param plugin The plugin which owns the JAR.
     * @param jio A JarIO handler.
     * @param directory The directory of where the templates exist. This is the
     * name of the package, but with the dots replaced by forward-slash. This
     * can be null/empty for the entire JAR.
     * @return True = loaded, false = could not load.
     */
    public synchronized boolean load(Plugin plugin, JarIO jio, String directory)
    {
        if(plugin == null)
            return false;
        try
        {
            final String ext = ".template";
            final int extLen = ext.length();
            String[] files = jio.getFiles(directory, ext, true, false);
            // Iterate each file
            String path, data;
            for(String f : files)
            {
                // Read data
                data = jio.fetchFileText(f);
                // Compute name
                path = directory == null || directory.length() == 0 ? f.substring(0, f.length()-extLen-1) : f.substring(directory.length()+1, f.length()-extLen);
                // Register as template
                if(!registerTemplate(plugin, path, data))
                    return false;
            }
            return true;
        }
        catch(JarIOException ex)
        {
            core.getLogging().log("[TEMPLATES] Could not load templates from jar directory '" + directory + "' ~ plugin [" + plugin.getUUID().getHexHyphens() + "]!", ex, Logging.EntryType.Error);
            return false;
        }
    }
    /**
     * Registers a template function.
     * 
     * @param functionName The name of the function; can be alpha-numeric,
     * contain hyphens, under-scrolls, forward slashes and dot. Backslash
     * is automatically translated to forward slash.
     * @param function
     * @return True if registered, false if failed (invalid format, path
     * already taken).
     */
    public synchronized boolean registerFunction(String functionName, TemplateFunction function)
    {
        functionName = functionName.replace("\\", "/");
        // Check the function format is correct and does not already exist
        if(!functionName.matches("^([a-zA-Z0-9\\.\\_\\-\\/]+)$") || functions.containsKey(functionName))
            return false;
        // Add the function to the collection
        functions.put(functionName, function);
        core.getLogging().log("[TEMPLATES] Registered template function '" + functionName + "' (" + function.getClass().getName() + ") ~ plugin [" + function.getPlugin().getHexHyphens() + "].", Logging.EntryType.Info);
        return true;
    }
    /**
     * Registers a template.
     * 
     * Note: if the template already exists within the collection, it will be
     * replaced.
     * 
     * @param plugin The plugin which owns the template; can be null.
     * @param path The path of the template; can contain alpha-numeric chars,
     * hyphen,underscroll, dot and forward-slash. Back-slash is automatically
     * translated to forward slash.
     * @param content The content of the template.
     * @return True if registered, false if failed (invalid format, path
     * already taken).
     */
    public synchronized boolean registerTemplate(Plugin plugin, String path, String content)
    {
        path = path.replace("\\", "/");
        // Check the template doesn't exist
        if(!path.matches("^([a-zA-Z0-9\\.\\_\\-\\/]+)$"))
        {
            core.getLogging().log("[TEMPLATES] Could not register template '" + path + "' ~ invalid path ~ plugin [" + (plugin != null ? plugin.getUUID().getHexHyphens() : "unknown") + "];", Logging.EntryType.Error);
            return false;
        }
        // Pass to loader for processing
        fmLoader.put(plugin, path, content);
        core.getLogging().log("[TEMPLATES] Registered template '" + path + "' ~ plugin [" + (plugin != null ? plugin.getUUID().getHexHyphens() : "unknown") + "].", Logging.EntryType.Info);
        return true;
    }
    /**
     * Renders content using template functions.
     * 
     * @param data The data from a web-request.
     * @param path The path of the template to be rendered.
     * @return The rendered data. Returns null if the specified template
     * cannot be found.
     */
    public synchronized String render(WebRequestData data, String path)
    {
        if(path == null)
            return null;
        try
        {
            // Fetch template
            Template t = fmConfig.getTemplate(path);
            if(t == null)
                return null;
            // Process template for the current context
            StringWriter sw = new StringWriter();
            t.process(data.getTemplateMap(), sw);
            // Process function calls
            StringBuilder buffer = new StringBuilder(sw.toString());
            Matcher m = pattFunctions.matcher(buffer);
            String str;
            String funcOut;
            int index;
            // Locate matches and replace them
            while(m.find())
            {
                str = m.group(0);
                index = buffer.indexOf(str);
                funcOut = functions.containsKey(m.group(1)) ? functions.get(m.group(1)).processCall(data, m.group(2)) : ("No function called '"+m.group(1)+"' exists!");
                buffer.replace(index, index+str.length(), funcOut);
            }
            return buffer.toString();
        }
        catch(IOException | TemplateException ex)
        {
            core.getLogging().log("[TEMPLATES] Could not render template for request '" + data.getRequestData().getRelativeUrl() + "', IP: '" + data.getRequestData().getIpAddress() + "'.", ex, Logging.EntryType.Warning);
            return null;
        }
    }
    // Methods - Mutators ******************************************************
        /**
     * Unloads all the templates and registered template functions.
     */
    public synchronized void clear()
    {
        fmLoader.clear();                   // Clear templates
        fmConfig.clearTemplateCache();      // Clear cache
        functions.clear();                  // Clear functions
    }
    public synchronized void remove(String path)
    {
        // Remove from collection
        fmLoader.remove(path);
        // Remove from cache
        try
        {
            fmConfig.removeTemplateFromCache(path);
        }
        catch(IOException ex)
        {
        }
    }
    /**
     * Unloads all the templates and registered template functions belonging to
     * a plugin.
     * 
     * Warning: this call could be expensive, due to clearing the template
     * cache; therefore use infrequently.
     * 
     * @param plugin The identifier of the plugin.
     */
    public synchronized void remove(Plugin plugin)
    {
        UUID pluginUuid = plugin.getUUID();
        // Remove templates
        fmLoader.remove(plugin);
        // Remove template functions
        Iterator<Map.Entry<String,TemplateFunction>> it = functions.entrySet().iterator();
        Map.Entry<String,TemplateFunction> func;
        UUID uuid;
        while(it.hasNext())
        {
            func = it.next();
            uuid = func.getValue().getPlugin();
            if(uuid == pluginUuid || (uuid != null && uuid.equals(pluginUuid)))
            {
                // Remove function from collection
                it.remove();
            }
        }
        // Clear cache
        fmConfig.clearTemplateCache();
    }
    // Methods - Accessors *****************************************************
    /**
     * @param path The path of a template.
     * @return True = template exists, false = does not exist.
     */
    public boolean containsTemplate(String path)
    {
        return fmLoader.contains(path);
    }
    /**
     * @param path The path of the template.
     * @return The instance representing the template or null.
     */
    public TemplateItem getTemplate(String path)
    {
        return fmLoader.getItem(path);
    }
    /**
     * @return The map used to store template functions.
     */
    public HashMap<String, TemplateFunction> getFunctionsMap()
    {
        return functions;
    }
}
