package pals.javasandbox;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * A simple application used to load compiled classes and run them within a
 * restricted environment.
 * 
 * The arguments expected:
 * -- 0:    The directory of class-files; the program will be allowed I/O to
 *          this directory (only).
 * -- 1:    The entry-point class.
 * -- 2:    The method to invoke; this must be static.
 * -- 3-n:  The arguments for the method; this is automatically parsed.
 *              Each argument should be with <type>=<value>
 *              Accepted types: int,long,float,double,string,char,boolean
 */
public class JavaSandbox
{
    public static void main(String[] args) throws InvocationTargetException
    {
        args = new String[]
        {
            "M:\\Dropbox\\UEA\\Modules\\Year 3\\CMPC3P2Y-13 - Project\\Codebase\\Plugins\\DefaultQuestionCriteriaHandlers\\temp",
            "test.Main",
            "main",
            "int=123",
            "double=3.614"
        };
        if(args.length < 3)
        {
            System.err.println("Invalid arguments.");
            return;
        }
        URL[] urlz;
        try
        {
            urlz = new URL[]{new File(args[0]).toURI().toURL()};
        }
        catch(MalformedURLException ex)
        {
            return;
        }
        // Load any classes used by this program elsewhere
        // -- We wont be able to access them after the SM enforcement
        try
        {
            Class.forName("pals.javasandbox.ParsedArgument");
        }
        catch(ClassNotFoundException ex)
        {
        }
        // Enforce security manager
        SandboxSecurityManager ssm;
        try
        {
            System.setSecurityManager((ssm = new SandboxSecurityManager(args[0])));
        }
        catch(IOException ex)
        {
            return;
        }
        // Create class-loader for directory
        // -- This has to be after the security-manager, so it can allow it
        // -- to be created via checkCreateClassLoader.
        URLClassLoader cl = new URLClassLoader(urlz);
        // Fetch class
        Class c;
        try
        {
            c = cl.loadClass(args[1]);
        }
        catch(ClassNotFoundException ex)
        {
            System.err.println("Could not find entry-point class.");
            return;
        }
        // Build arguments
        Object[] objs;
        Class[] classes;
        try
        {
            objs = new Object[args.length - 3];
            classes = new Class[args.length - 3];
            ParsedArgument pa;
            for(int i = 3; i < args.length; i++)
            {
                pa = ParsedArgument.parse(args[i]);
                objs[i-3] = pa.getArgValue();
                classes[i-3] = pa.getArgClass();
            }
        }
        catch(IllegalArgumentException ex)
        {
            System.err.println("Could not parse entry-point arguments.");
            return;
        }
        // Fetch the required method
        Method meth = null;
        try
        {
            meth = c.getMethod(args[2], classes);
        }
        catch(NoSuchMethodException ex)
        {
            System.err.println("Could not find entry-point method.");
            return;
        }
        // Invoke method
        try
        {
            meth.invoke(null, objs);
        }
        catch(IllegalAccessException ex)
        {
            System.err.println("Could not access entry-point method.");
            return;
        }
        catch(IllegalArgumentException ex)
        {
            System.err.println("Incorrect parameters for entry-point method.");
            return;
        }
    }
}
