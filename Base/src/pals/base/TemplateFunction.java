package pals.base;

import pals.base.web.WebRequestData;

/**
 * The base-class used by all template functions.
 */
public abstract class TemplateFunction
{
    // Fields ******************************************************************
    private UUID plugin;    // The owner of this function.
    // Methods - Constructor ***************************************************
    public TemplateFunction(Plugin plugin)
    {
        this.plugin = plugin.getUUID();
    }
    // Methods *****************************************************************
    /**
     * Used to process a function call from a template.
     * 
     * @param data The data for the current web-request.
     * @param args Any arguments, as parsed from a template.
     * @return String result from function.
     */
    public String processCall(WebRequestData data, String args)
    {
        return null;
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The owner of this function.
     */
    public UUID getPlugin()
    {
        return plugin;
    }
}
