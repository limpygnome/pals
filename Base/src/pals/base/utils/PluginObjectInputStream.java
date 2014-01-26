package pals.base.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import pals.base.NodeCore;
import pals.base.Plugin;

/**
 * An extension of ObjectInputStream to resolve classes available from plugin
 * JARs, which have not been loaded into the runtime of the base library.
 */
public class PluginObjectInputStream extends ObjectInputStream
{
    private NodeCore core;
    public PluginObjectInputStream(NodeCore core, InputStream is) throws IOException
    {
        super(is);
        this.core = core;
    }
    @Override
    protected Class<?> resolveClass(ObjectStreamClass osc) throws IOException, ClassNotFoundException
    {
        String className = osc.getName();
        // Remove [L<class name>; for indicating a class for an array
        //if(className.startsWith("[L") && className.endsWith(";") && className.length() > 3)
        //    className = className.substring(2, className.length()-1);
        // Attempt to fetch from super first
        try
        {
            return super.resolveClass(osc);
        }
        catch(ClassNotFoundException ex)
        {
        }
        // Iterate each plugin for the class
        for(Plugin p : core.getPlugins().getPlugins())
        {
            try
            {
                return Class.forName(className, false, p.getJarIO().getRawLoader());
            }
            catch(ClassNotFoundException ex)
            {
            }
        }
        throw new ClassNotFoundException("PluginObjectInputStream - cannot find class '"+className+"'");
    }
}
