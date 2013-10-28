package pals.base.testing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
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
            return;
        }
        // Invoke without reflection
        try
        {
            String data = obj.test();
            System.out.println("Data from non-reflected call: '" + data + "'.");
            data = obj.test(5, 6);
            System.out.println("Data from non-reflected call 2: '" + data + "'.");
        }
        catch(Exception ex)
        {
            System.out.println("Failed to call method without reflection: " + ex.getMessage());
            return;
        }
        // Test loading a file from the jar
        InputStream fs = loader.getResourceAsStream("example/files/test.txt");
        try
        {
            if(fs == null)
                throw new Exception("fs is null.");
            InputStreamReader isr = new InputStreamReader(fs);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder data = new StringBuilder();
            int c;
            while((c = br.read()) != -1)
                data.append((char)c);
            System.out.println("Data: '" + data.toString() + "'");
        }
        catch(Exception ex)
        {
            System.out.println("Error reading jar file: " + ex.getMessage());
            return;
        }
        // List files in a jar
        try
        {
            System.out.println("Files in jar:");
            JarInputStream jis = new JarInputStream(new FileInputStream(path));
            JarEntry je;
            while((je = jis.getNextJarEntry()) != null)
            {
                if(!je.isDirectory())
                    System.out.println("- '" + je.getName() + "'");
            }
        }
        catch(Exception ex)
        {
            System.out.println("Error reading files in jar: " + ex.getMessage());
            return;
        }
    }
}
