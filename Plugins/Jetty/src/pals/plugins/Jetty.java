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

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import pals.base.NodeCore;
import pals.base.Plugin;
import pals.base.Settings;
import pals.base.UUID;
import pals.base.Version;
import pals.base.database.Connector;
import pals.base.utils.JarIO;

/**
 * A plugin which embeds the Jetty embedded Java web-server.
 */
public class Jetty extends Plugin
{
    // Constants ***************************************************************
    private final String LOGGING_ALIAS = "[PALS] Web Serv.";
    // Fields ******************************************************************
    private Server httpServer;
    // Methods - Constructors **************************************************
    public Jetty(NodeCore core, UUID uuid, JarIO jario, Version version, Settings settings, String jarPath)
    {
        super(core, uuid, jario, version, settings, jarPath);
        // Setup server
        setup(8084);
    }
    // Methods - Debugging *****************************************************
    public static void main(String[] args) throws InterruptedException
    {
        Jetty j = new Jetty(null, null, null, null, null, null);
        j.setup(8084);
        j.webStart("../../Website/build/web", "../../Website/web/WEB-INF/web.xml");
        j.httpServer.join();
    }
    // Methods - Handlers ******************************************************
    @Override
    public boolean eventHandler_pluginInstall(NodeCore core, Connector conn)
    {
        return true;
    }
    @Override
    public boolean eventHandler_pluginUninstall(NodeCore core, Connector conn)
    {
        return true;
    }
    @Override
    public boolean eventHandler_pluginLoad(NodeCore core)
    {
        return webStart("../Website/build/web", "../Website/web/WEB-INF/web.xml");
    }
    @Override
    public void eventHandler_pluginUnload(NodeCore core)
    {
        webStop();
    }
    @Override
    public String getTitle()
    {
        return "PALS: Embedded Web Server";
    }
    // Methods *****************************************************************
    private void setup(int port)
    {
        httpServer = new Server(8084);
    }
    /**
     * @param path The path of the web application's files.
     * @param webXMLPath The path of the web.xml (descriptor) file.
     * @return Indicates if the web-server was started.
     */
    public boolean webStart(String path, String webXMLPath)
    {
        // Create context of the web application
        WebAppContext context = new WebAppContext();
        context.setContextPath("/");
        context.setDescriptor(webXMLPath);
        context.setResourceBase(path);
        context.setParentLoaderPriority(true);
        try
        {
            ArrayList<URL> locs = new ArrayList<>();
            locs.add(new File(path+"/WEB-INF/classes").toURI().toURL());
//            // Add each jar in the lib folder
//            File[] jars = Files.getAllFiles(new File(getJarLocation()).getParentFile().getPath()+"/lib", false, true, ".jar", true);
//            for(File jar : jars)
//                locs.add(jar.toURI().toURL());
            
            // Add jar files
            String basePath = new File(getJarLocation()).getParentFile().getPath().replace("\\", "/")+"/lib/";
            locs.add(new File(basePath+"jetty-all-9.1.1.v20140108.jar").toURI().toURL());
            //locs.add(new File(basePath+"javaee-api-7.0.jar").toURI().toURL());
            //locs.add(new File(basePath+"javaee-web-api-7.0.jar").toURI().toURL());
            locs.add(new File(basePath+"javax.servlet-3.0.0.v201112011016.jar").toURI().toURL());
            // Set the class-loader
            context.setClassLoader(new URLClassLoader(
                    locs.toArray(new URL[locs.size()])
            ));
            Class.forName("PALS_Servlet", false, context.getClassLoader());
            System.err.println("LOADED.");
        }
        catch(Exception ex)
        {
            System.err.println("FAILED TO FIND CLASS... at '"+new File(getJarLocation()).getParentFile().getPath()+"/lib"+"'");
            return false;
        }
        // Start server
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
    /**
     * @return Indicates if the web-server was stopped.
     */
    public boolean webStop()
    {
        try
        {
            httpServer.stop();
        }
        catch(Exception ex)
        {
            return false;
        }
        return true;
    }
}
