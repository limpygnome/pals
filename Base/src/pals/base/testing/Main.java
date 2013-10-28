package pals.base.testing;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import pals.base.Plugin;

/**
 *
 * @author limpygnome
 */
public class Main
{
    public static void main(String[] args)
    {
        // Load example jar plugin and check we can access the example plugin class
        String path = "../Plugins/Example/dist/Example.jar";
        
        File file = new File(path);
        URL url = null;
        // Create URL to path
        try
        {
            url = file.toURI().toURL();
        }
        catch(Exception ex)
        {
            System.err.println("URI creation failed.");
            return;
        }
        // Load the class from the jar
        URLClassLoader loader = URLClassLoader.newInstance(new URL[]{url});
        Class example = null;
        try
        {
            example = loader.loadClass("pals.plugins.Example");
        }
        catch(Exception ex)
        {
            System.err.println("Failed to load class.");
            return;
        }
        // Load an instance of the class
        Plugin obj = null;
        try
        {
            obj = (Plugin)example.newInstance();
            System.out.println("Loaded instance of: '" + obj.getClass().getName() + "'.");
        }
        catch(Exception ex)
        {
            System.err.println("Failed to load instance of class!");
            return;
        }
        // Load the test method
        Method method = null;
        try
        {
            method = example.getMethod("test", new Class[]{});
            System.out.println("Loaded method: '" + method.getName() + "'.");
        }
        catch(NoSuchMethodException ex)
        {
            System.err.println("No such method.");
        }
        catch(Exception ex)
        {
            System.err.println("Failed to load method.");
            return;
        }
        // Invoke test method
        try
        {
            String data = (String)method.invoke(obj);
            System.out.println("Data from reflected call: '" + data + "'.");
        }
        catch(Exception ex)
        {
            System.err.println("Failed to invoke method ~ '" + ex.getMessage() + "'.");
        }
        // Invoke without reflection
        try
        {
            String data = obj.test();
            System.out.println("Data from non-reflected call: '" + data + "'.");
        }
        catch(Exception ex)
        {
            System.out.println("Failed to call method without reflection: " + ex.getMessage());
        }
    }
}
