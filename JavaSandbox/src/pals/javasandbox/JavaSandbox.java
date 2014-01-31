package pals.javasandbox;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;

/**
 * A simple application used to load compiled classes and run them within a
 * restricted environment.
 * 
 * The arguments expected:
 * -- 0:    The directory of class-files; the program will be allowed I/O to
 *          this directory (only).
 * -- 1:    The entry-point class.
 * -- 2:    The method to invoke; this must be static.
 * -- 3:    List of white-listed classes, or 0 for no white-listing.
 * -- 4:    Output mode (1 or 0) - outputs the value from the method.
 * -- 5:    Timeout before self-terminating.
 * -- 6-n:  The arguments for the method; this is automatically parsed.
 *              Each argument should be with <type>=<value>
 *              Accepted types: refer to ParsedArgument class.
 */
public class JavaSandbox
{
    // Fields - Static *********************************************************
    public static boolean   modeDebug = false,
                            modeOutput = false;
    public static Thread    threadWatcher = null;
    public static int       timeout = -1;
    // Methods - Entry-Point ***************************************************
    public static void main(String[] args) throws InvocationTargetException
    {
        if(args.length < 5)
        {
            System.err.println("Invalid arguments.");
            return;
        }
        // Setup outut-mode
        modeOutput = args[4].equals("1");
        if(modeDebug)
            System.out.println("[DEBUG] Debug-mode has been enabled.");
        // Parse time-out value
        try
        {
            timeout = Integer.parseInt(args[5]);
        }
        catch(NumberFormatException ex)
        {
            printDebugData(ex);
            timeout = -1;
        }
        if(timeout <= 0)
        {
            System.err.println("Invalid time-out value.");
            return;
        }
        // Setup the thread to monitor the sandbox
        threadWatcher = new Thread()
        {
            public void run()
            {
                try
                {
                    Thread.sleep(timeout);
                }
                catch(InterruptedException ex)
                {
                }
                System.exit(1);
            }
        };
        // Define the URLs used by the class-loader, which is just the directory
        // of the class-files (at present).
        URL[] urls;
        try
        {
            if(modeDebug)
                System.out.println("[DEBUG] Loading classes at '"+new File(args[0]).getCanonicalPath()+"'.");
            urls = new URL[]{new File(args[0]).toURI().toURL()};
        }
        catch(IOException ex)
        {
            System.err.println("Failed to setup URLs for class-loader.");
            printDebugData(ex);
            return;
        }
        // Load any classes used by this program elsewhere
        // -- We wont be able to access them after the SM enforcement
        try
        {
            Class.forName("pals.javasandbox.ParsedArgument");
            Class.forName("pals.javasandbox.SandboxRestrictedLoader");
        }
        catch(ClassNotFoundException ex)
        {
            System.err.println("Failed to load internal classes.");
            printDebugData(ex);
            return;
        }
        // Enforce security manager
        SandboxSecurityManager ssm;
        try
        {
            System.setSecurityManager((ssm = new SandboxSecurityManager(new File(args[0]).getCanonicalPath())));
        }
        catch(IOException ex)
        {
            System.err.println("Failed to setup security-manager.");
            printDebugData(ex);
            return;
        }
        // Create class-loader for directory
        // -- This has to be after the security-manager, so it can allow it
        // -- to be created via checkCreateClassLoader.
        SandboxRestrictedLoader srl = new SandboxRestrictedLoader(urls);
        // Add white-list of allowed classes
        {
            String whiteList = args[3];
            if(whiteList.length() > 0 && !whiteList.equals("0"))
            {
                srl.setWhiteListEnabled(true);
                for(String className : whiteList.split(","))
                {
                    srl.whiteListAdd(className);
                    if(modeDebug)
                        System.out.println("[DEBUG] Added class '"+className+"' to whitelist.");
                }
                if(modeDebug)
                    System.out.println("[DEBUG] Class white-listing has been enabled.");
            }
            else
                srl.setWhiteListEnabled(false);
        }
        // Fetch class
        Class c;
        try
        {
            c = srl.loadClass(args[1]);
        }
        catch(SecurityException ex)
        {
            System.err.println("Attempted to load restricted class '"+args[1]+"'.");
            printDebugData(ex);
            return;
        }
        catch(ClassNotFoundException ex)
        {
            if(ex instanceof WhitelistException)
                System.err.println("The class '"+((WhitelistException)ex).getClassName()+"' is prohibited!");
            else
                System.err.println("Could not find entry-point class '"+args[1]+"'.");
            printDebugData(ex);
            return;
        }
        // Build arguments
        Object[] objs;
        Class[] classes;
        try
        {
            final int argsStartIndex = 6;
            objs = new Object[args.length - argsStartIndex];
            classes = new Class[args.length - argsStartIndex];
            ParsedArgument pa;
            for(int i = argsStartIndex; i < args.length; i++)
            {
                pa = ParsedArgument.parse(args[i]);
                objs[i-argsStartIndex] = pa.getArgValue();
                classes[i-argsStartIndex] = pa.getArgClass();
                if(modeDebug)
                    System.out.println("[DEBUG] Argument "+(i-argsStartIndex)+": "+pa.getArgClass().getName()+"="+pa.getArgValue());
            }
        }
        catch(IllegalArgumentException ex)
        {
            System.err.println("Could not parse entry-point arguments.");
            printDebugData(ex);
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
            System.err.println("Could not find entry-point method '"+args[2]+"'.");
            printDebugData(ex);
            return;
        }
        // Start the time-out thread
        threadWatcher.start();
        // Invoke method
        try
        {
            Object obj = meth.invoke(null, objs);
            if(modeOutput)
                System.out.println(obj != null ? obj : "null");
        }
        catch(SecurityException ex)
        {
            System.err.println("Attempted to perform prohibited action during runtime.");
            printDebugData(ex);
            return;
        }
        catch(IllegalAccessException ex)
        {
            System.err.println("Could not access entry-point method.");
            printDebugData(ex);
            return;
        }
        catch(IllegalArgumentException ex)
        {
            System.err.println("Incorrect parameters for entry-point method.");
            printDebugData(ex);
            return;
        }
        // Kill the JVM
        System.exit(0);
    }
    public static void printDebugData(Exception ex)
    {
        if(modeDebug)
        {
            System.err.println("DEBUG EXCEPTION INFORMATION");
            System.err.println("*******************************************************************");
            System.err.println("Exception occurred; type: '"+ex.getClass().getName()+"'");
            System.err.println(ex.getMessage());
            System.err.println("*******************************************************************");
            System.err.println("Stack-trace:");
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            pw.flush();
            System.err.println(sw.toString());
        }
    }
}
