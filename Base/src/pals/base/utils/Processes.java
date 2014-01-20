package pals.base.utils;

import java.io.IOException;
import pals.base.NodeCore;

/**
 * A wrapper for launching processes.
 */
public class Processes
{
    public enum OS
    {
        Unknown,
        Windows,
        Linux
    }
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
     * Creates a new process and applies any authentication specified in this
     * node's configuration/settings.
     * 
     * @param core Current instance of the core.
     * @param command The command to execute.
     * @return The process created; null if it failed to be created.
     */
//    public static Process create(NodeCore core, String command)
//    {
//        // Fetch credentials
//        
//        // Spawn new process
//        try
//        {
//            OS os = getOS();
//            switch(os)
//            {
//                case Windows:
//                    Runtime.getRuntime().exec("runas");
//                    break;
//                case Linux:
//                    Runtime.getRuntime().exec("runuser");
//                    break;
//                default:
//                    return null;
//            }
//        }
//        catch(IOException ex)
//        {
//            return null;
//        }
//        Process p;
//        // No timeout available; use threading...
//    }
}
