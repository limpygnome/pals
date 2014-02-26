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

import java.io.IOException;
import pals.base.Logging;
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
    private static  Process proc = null;
    private         Thread threadShutdown;
    // Methods - Constructors **************************************************
    public Jetty(NodeCore core, UUID uuid, JarIO jario, Version version, Settings settings, String jarPath)
    {
        super(core, uuid, jario, version, settings, jarPath);
        
        threadShutdown = new Thread(){
            @Override
            public void run()
            {
                jettyStop();
            }
        };
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
        Runtime.getRuntime().addShutdownHook(threadShutdown);
        jettyStart();
        return true;
    }
    @Override
    public void eventHandler_pluginUnload(NodeCore core)
    {
        jettyStop();
        Runtime.getRuntime().removeShutdownHook(threadShutdown);
    }
    @Override
    public String getTitle()
    {
        return "PALS: Embedded Web Server";
    }
    // Methods *****************************************************************
    public synchronized void jettyStart()
    {
        try
        {
            if(newJarLocation != null)
                proc = Runtime.getRuntime().exec("java -jar \""+newJarLocation+"\" \"8084\" \"../Website/build/web\"");
            else
                getCore().getLogging().log(LOGGING_ALIAS, "Unable to start Jetty; new JAR location undefined.", Logging.EntryType.Error);
        }
        catch(IOException ex)
        {
            getCore().getLogging().logEx(LOGGING_ALIAS, "Unable to start Jetty server.", ex, Logging.EntryType.Error);
        }
    }
    public synchronized void jettyStop()
    {
        proc.destroy();
        proc = null;
    }
}
