package pals.base.utils;

import java.io.File;
import java.io.IOException;
import pals.base.NodeCore;

/**
 * A cross-platform wrapper for launching processes.
 */
public class PalsProcess
{
    // Enums *******************************************************************
    public enum OS
    {
        Unknown,
        Windows,
        Linux
    }
    // Fields ******************************************************************
    private Process         proc;
    private ProcessBuilder  pb;
    // Methods - Constructors **************************************************
    private PalsProcess(ProcessBuilder pb)
    {
        this.pb = pb;
        this.proc = null;
    }
    // Methods *****************************************************************
    /**
     * @return True if the process started, false if it failed to start.
     */
    public boolean start()
    {
        try
        {
            proc = pb.start();
            return true;
        }
        catch(IOException ex)
        {
            return false;
        }
    }
    /**
     * @return Indicates if the process has terminated.
     */
    public boolean hasExited()
    {
        try
        {
            proc.exitValue();
            return true;
        }
        catch(IllegalThreadStateException ex)
        {
            return false;
        }
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The underlying process.
     */
    public Process getProcess()
    {
        return proc;
    }
    /**
     * @return The underlying process-builder.
     */
    public ProcessBuilder getProcessBuilder()
    {
        return pb;
    }
    // Methods - Static ********************************************************
    /**
     * @return The operating system this JVM is currently operating on.
     */
    public static OS getOS()
    {
        String os = System.getProperty("os.name").toLowerCase();
        if(os.indexOf("win") != -1)
            return OS.Windows;
        else if(os.indexOf("linux") != -1)
            return OS.Linux;
        else
            return OS.Unknown;
    }
    /**
     * Creates a new process and applies the authentication specified in this
     * node's configuration/settings.
     * 
     * @param core Current instance of the core.
     * @param fullPath The full path of the program  to execute.
     * @param args The arguments to be passed to the program.
     * @return The process created; null if it failed to be created.
     */
    public static PalsProcess create(NodeCore core, String fullPath, String args)
    {
        // Escape quotes in args
        args = args.replace("\"", "\\\"");
        // Fetch credentials
        String username = core.getSettings().getStr("processes/credentials/username");
        String password = core.getSettings().getStr("processes/credentials/password");
        // Build the process
        ProcessBuilder pb = new ProcessBuilder();
        try
        {
            OS os = getOS();
            switch(os)
            {
                case Windows:
                    // Calculate absolute path to tool for switching users
                    File f = new File(core.getSettings().getStr("tools/windows_user_tool/path"));
                    // Execute
                    System.out.println("\""+f.getCanonicalPath()+"\" \""+username+"\" \""+password+"\" \""+core.getSettings().getInt("tools/windows_user_tool/timeout_ms", 10000)+"\" \""+fullPath+"\" \""+args+"\"");
                    pb.command("\""+f.getCanonicalPath()+"\" \""+username+"\" \""+password+"\" \""+core.getSettings().getInt("tools/windows_user_tool/timeout_ms", 10000)+"\" \""+fullPath+"\" \""+args+"\"");
                    break;
                case Linux:
                    pb.command("sshpass -p \""+password+"\" ssh "+username+"@localhost \""+fullPath+(args.length() > 0 ? " "+args : "")+"\"");
                    break;
                default:
                    return null;
            }
        }
        catch(IOException ex)
        {
            return null;
        }
        // Setup redirection
        pb.redirectErrorStream(true);
        return new PalsProcess(pb);
    }
}
