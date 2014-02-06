import pals.base.NodeCore;

/**
 * The execution point of the node program.
 */
public class Main
{
    // Fields - Static *********************************************************
    private static NodeCore core;
    // Methods - Entry-Points **************************************************
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
        // Developers mode - change plugins dir
        if(devMode)
        {
            System.out.println("Warning: running node in developer-mode!");
            core.setPathPlugins("../Plugins");
        }
        // Start the code...
        System.out.println("Starting node core...");
        if(!core.start())
        {
            System.err.println("Failed to start core...");
            return;
        }
        // Wait for the node to shutdown
        while(core.getState() != NodeCore.State.Shutdown && core.getState() != NodeCore.State.Failed)
        {
            try
            {
                core.waitStateChange();
            }
            catch(InterruptedException ex)
            {
                System.err.println("Node error ~ InterruptedException ~ core.waitStateChange ~ '" + ex.getMessage() + "'!");
            }
        }
        System.exit(0);
    }
}
