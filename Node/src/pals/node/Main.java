package pals.node;

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
    Authors:    Marcus Craske           <limpygnome@gmail.com>
    ----------------------------------------------------------------------------
*/
import pals.base.NodeCore;

/**
 * The execution point of the node program.
 * 
 * @version 1.0
 */
public class Main
{
    // Fields - Static *********************************************************
    private static NodeCore core;
    // Methods - Entry-Points **************************************************
    /**
     * The entry-point of this application, which creates an instance of a
     * PALS node.
     * 
     * @param args Environment arguments.
     * @since 1.0
     */
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
