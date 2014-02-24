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
            System.err.println("PalsProcess ~ failed to start process ~ "+ex.getMessage());
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
     * @param path A file-system path to be passed to the {@link #create(pals.base.NodeCore, java.lang.String, java.lang.String)}.
     * @return A formatted path.
     */
    public static String formatPath(String path)
    {
        switch(getOS())
        {
            case Linux:
                return path.replace(" ", "\\ ");
            default:
                return path;
        }
    }
    /**
     * Creates a new process and applies the authentication specified in this
     * node's configuration/settings.
     * 
     * Warning:
     * All paths, including fullPath and any paths in args, should be formatted
     * with the method {@link #formatPath(java.lang.String)}.
     * 
     * @param core Current instance of the core.
     * @param workingDir The working directory of the process.
     * @param fullPath The full path of the program  to execute.
     * @param args The arguments to be passed to the program.
     * @return The process created; null if it failed to be created.
     */
    public static PalsProcess create(NodeCore core, String workingDir, String fullPath, String[] args)
    {
        File fWD = new File(workingDir);
        // Fetch credentials
        String username = core.getSettings().getStr("processes/credentials/username");
        String password = core.getSettings().getStr("processes/credentials/password");
        // Build the process
        ProcessBuilder pb = new ProcessBuilder();
        try
        {
            OS os = getOS();
            String[] cmd;
            switch(os)
            {
                case Windows:
                    // Calculate absolute path to tool for switching users
                    File f = new File(core.getSettings().getStr("tools/windows_user_tool/path"));
                    // Escape args
                    cmd = new String[]
                    {
                        f.getCanonicalPath(),
                        username,
                        password,
                        Integer.toString(core.getSettings().getInt("tools/windows_user_tool/timeout_ms", 10000)),
                        fullPath,
                        windowsArgs(args)
                    };                    
                    break;
                case Linux:
                    cmd = new String[]
                    {
                        "/usr/bin/sshpass",
                        "-p",
                        password,
                        "ssh",
                        "-o",
                        "StrictHostKeyChecking=no",
                        username+"@localhost",
                        fullPath
                    };
                    cmd = Misc.arrayMerge(String.class, cmd, args);
                    break;
                default:
                    return null;
            }
            //printDebug(cmd);
            pb.directory(fWD);
            pb.command(cmd);
        }
        catch(IOException ex)
        {
            return null;
        }
        // Setup redirection
        pb.redirectErrorStream(true);
        return new PalsProcess(pb);
    }
    private static void printDebug(String[] args)
    {
        StringBuilder sb = new StringBuilder("[Debug][PalsProcess] Args: ");
        if(args.length > 0)
        {
            for(String s : args)
                sb.append("\"").append(s).append("\" ");
            sb.deleteCharAt(sb.length()-1);
        }
        else
            sb.append("[No arguments]");
        System.err.println(sb.toString());
    }
    private static String windowsArgs(String[] args)
    {
        StringBuilder sb = new StringBuilder();
        for(String s : args)
            sb.append("\\\"").append(s).append("\\\" ");
        if(sb.length() > 1)
            sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }
}
