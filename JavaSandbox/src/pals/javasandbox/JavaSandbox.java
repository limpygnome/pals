/*
    The MIT License (MIT)

    Copyright (c) 2014 Marcus Craske <limpygnome@gmail.com>

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in
    all copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
    THE SOFTWARE.
    ----------------------------------------------------------------------------
    Version:    1.0
    Authors:    Marcus Craske           <limpygnome@gmail.com>
    ----------------------------------------------------------------------------
*/
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
 * -- 6:    Indicates if this is a sub-process.
 * -- 7-n:  The arguments for the method; this is automatically parsed.
 *              Each argument should be with <type>=<value>
 *              Accepted types: refer to ParsedArgument class.
 */
public class JavaSandbox
{
    // Fields - Static *********************************************************
    public static boolean   modeDebug = false,       // Has to be changed and compiled
                            modeOutput = false;
    public static Thread    threadWatcher = null;
    public static int       timeout = -1;
    // Methods - Entry-Point ***************************************************
    public static void main(String[] args) throws Throwable
    {
        if(args.length < 6)
        {
            System.err.println("Invalid arguments.");
            return;
        }
        // Check we're in the correct working dir, but only if we're not a sub-process
        File cf = new File(args[0]);
        if(!args[6].equals("1"))
        {
            if(modeDebug)
                System.out.println("[DEBUG] Checking working directory...");
            if(!cf.exists())
            {
                System.out.println("[DEBUG] Directory does not exist.");
                return;
            }
            String  cfPath = cf.getCanonicalPath().replace("\\", "/"),
                    wdPath = new File("").getCanonicalPath().replace("\\", "/");
            
            if(modeDebug)
                System.out.println("[DEBUG] Working directory is '"+wdPath+"'.");
            
            if(!cfPath.equals(wdPath))
            {
                if(modeDebug)
                    System.out.println("[DEBUG] Working directory is incorrect, spawning sub-process...");
                // Fetch path of this JAR
                String jarPath = JavaSandbox.class.getProtectionDomain().getCodeSource().getLocation().getPath();
                if(modeDebug)
                    System.out.println("[DEBUG] JAR path is '"+jarPath+"'.");
                // Prepare child process with changed working directory
                ProcessBuilder pb = new ProcessBuilder();
                int argOffset = 3;
                String[] args2 = new String[args.length+argOffset];
                args2[0] = "java";
                args2[1] = "-jar";
                args2[2] = jarPath;
                for(int i = 0; i < args.length; i++)
                    args2[i+argOffset] = args[i];
                args2[6+argOffset] = "1"; // Set flag as child process to avoid infinite creation, worst-case.
                pb.command(args2);
                pb.directory(cf);
                // Redirect I/O
                pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
                pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                // Start the process and wait for it to terminate
                Process proc = pb.start();
                if(modeDebug)
                    System.out.println("[DEBUG] Started sub-process.");
                boolean running;
                do
                {
                    // Check if the process has exited
                    try
                    {
                        proc.exitValue();
                        running = false;
                    }
                    catch(IllegalThreadStateException ex)
                    {
                        running = true;
                    }
                    // Sleep...
                    try
                    {
                        Thread.sleep(5);
                    }
                    catch(InterruptedException ex)
                    {
                    }
                }
                while(running);
                return;
            }
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
                System.out.println("[DEBUG] Loading classes at '"+cf.getCanonicalPath()+"'.");
            urls = new URL[]{cf.toURI().toURL()};
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
                System.err.println("Could not find class '"+args[1]+"'.");
            printDebugData(ex);
            return;
        }
        // Build arguments
        Object[] objs;
        Class[] classes;
        try
        {
            final int argsStartIndex = 7;
            objs = new Object[args.length - argsStartIndex];
            classes = new Class[args.length - argsStartIndex];
            ParsedArgument pa;
            if(modeDebug)
                System.out.println("[DEBUG] Parsing arguments...");
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
            System.err.println("Could not parse entry-point arguments ~ "+ex.getMessage()+".");
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
        catch(InvocationTargetException ex)
        {
            Throwable e = ex.getTargetException();
            if(e != null)
            {
                // Print the exception
                System.out.println("Exception: "+e.getClass().getName()+" - cause: "+(e.getMessage() != null ? e.getMessage().replace("\r", "").replace("\n", " ") : "null"));
                // Write stack-trace to stderr up until reflection part
                // -- To avoid confusing the student/user and for security
                System.err.println("Stack Trace:");
                StackTraceElement[] sframes = e.getStackTrace();
                int pos = 0;
                while(pos != -1 && pos < sframes.length)
                {
                    if(sframes[pos].getMethodName().startsWith("invoke"))
                        pos = -1;
                    else
                        System.err.println("at "+sframes[pos++]);
                }
            }
        }
        // Ensure standard output is flushed, although this should be redundant
        System.out.println("javasandbox-end-of-program");
        System.in.read();
        System.out.flush();
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
