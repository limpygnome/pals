
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import pals.base.NodeCore;

/**
 * The execution point of the node program.
 */
public class Main
{
    private static NodeCore core;
    public static void main(String[] args)
    {
        System.out.println("Node starting...");
        // Get an instance of the core
        core = NodeCore.getInstance();
        // Read launch arguments
        boolean devMode = false;
        for(String arg : args)
        {
            if(arg.equals("-dev"))
                devMode = true;
        }
        // Developers mode - copy plugins
        if(devMode)
        {
            System.out.println("Warning: developer mode enabled!");
            try
            {
                File root = new File("..");
                String rootPath = root.getCanonicalPath();
                int len;
                String dirr;
                String file;
                System.out.println("[DEBUG] Copying plugins at '" + rootPath + "/Plugins'...");
                for(File dir : pals.base.utils.Files.getAllFiles(rootPath + "/Plugins", true, false, ".jar", false))
                {
                    System.out.println("[DEBUG] Copying plugin at '" + dir.getPath() + "'...");
                    dirr = dir.getCanonicalPath() + "/dist";
                    len = dirr.length()+1;
                    // Copy all the files in the dist dir
                    for(File sf : pals.base.utils.Files.getAllFiles(dirr, false, true, null, true))
                    {
                        file = sf.getPath().substring(len);
                        System.out.println("[DEBUG] - '" + dirr + "/" + file + "', 'Node/_plugins/" + dir.getName() + "/" + file  + "'");
                        pals.base.utils.Files.fileCopy(dirr + "/" + file, rootPath + "/Node/_plugins/" + dir.getName() + "/" + file, true);
                    }
                }
            }
            catch(IOException ex)
            {
                System.out.println("[DEBUG] Failed to copy ~ '" + ex.getMessage() + "'.");
            }
        }
        // Start the code...
        System.out.println("Starting node core...");
        if(!core.start())
        {
            System.err.println("Failed to start core...");
            return;
        }
        // Wait for the node to terminate
        while(true)
            ;
    }
}
