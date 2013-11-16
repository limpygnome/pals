package pals.testing;

import pals.base.Plugin;
import pals.base.utils.JarIO;
import pals.base.utils.JarIOException;

public class Test_Plugin
{
    public static void main(String[] args)
    {
        // Load example jar plugin and check we can access the example plugin class
        String path = "../Plugins/Example/dist/Plugin__Example.jar";
        
        try
        {
            JarIO jar = JarIO.open(path);
            System.out.println("Path: '" + jar.getPath() + "'.");
            System.out.println("Files:");
            for(String str : jar.getFiles(null, true, false))
                System.out.println("- '" + str + "'");
            Plugin example = (Plugin)jar.fetchClassType("pals.plugins.Example").newInstance();
            //System.out.println("Output from test method: '" + example.test() + "'"); // commented-out -> no longer exists!
        }
        catch(InstantiationException | IllegalAccessException ex)
        {
            System.out.println("Cannot create class: '" + ex.getMessage() + "'!");
        }
        catch(JarIOException ex)
        {
            System.err.println(ex.getReason().toString() + " - " + ex.getMessage());
        }
    }
}
