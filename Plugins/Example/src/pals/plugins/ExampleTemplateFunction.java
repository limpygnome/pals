package pals.plugins;

import pals.base.Plugin;
import pals.base.TemplateFunction;
import pals.base.web.WebRequestData;

/**
 * An example template function.
 */
public class ExampleTemplateFunction extends TemplateFunction
{
    public ExampleTemplateFunction(Plugin plugin)
    {
        super(plugin);
    }
    @Override
    public String processCall(WebRequestData data, String args)
    {
        return "[hello from example template function]";
    }
}
