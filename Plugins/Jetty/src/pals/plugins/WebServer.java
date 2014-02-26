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
package pals.plugins;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 * The entry-point of this plugin, when launched as an external process.
 * 
 * Arguments expected:
 * - port
 * - path of website
 */
public class WebServer
{
    // Fields ******************************************************************
    private static Server   httpServer = null;
    private static String   path = null;
    private static int      port = 8084;
    private static Thread   th = null;
    private static Process  proc;
    // Methods - Entry-Points **************************************************
    public static void main(String[] args)
    {
        // Parse args
        if(args.length != 2)
        {
            System.err.println("Invalid arguments; first argument must be port, second argument must be path.");
            return;
        }
        try
        {
            port = Integer.parseInt(args[0]);
        }
        catch(NumberFormatException ex)
        {
        }
        path = args[1];
        // Start the web-server
        start();
        try
        {
            httpServer.join();
        }
        catch(InterruptedException ex)
        {
            System.exit(1);
        }
    }
    private static boolean start()
    {
        httpServer = new Server(port);
         // Create context of the web application
        WebAppContext context = new WebAppContext();
        context.setContextPath("/");
        context.setDescriptor(path+"/WEB-INF/web.xml");
        context.setResourceBase(path);
        context.setParentLoaderPriority(true);
        // Start the server
        httpServer.setHandler(context);
        try
        {
            httpServer.start();
        }
        catch(Exception ex)
        {
            return false;
        }
        return true;
    }
    private static boolean stop()
    {
        try
        {
            if(httpServer != null)
                httpServer.stop();
        }
        catch(Exception ex)
        {
            return false;
        }
        return true;
    }
}
