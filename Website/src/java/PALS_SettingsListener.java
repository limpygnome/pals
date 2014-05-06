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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import pals.base.NodeCore;
import pals.base.Settings;
import pals.base.SettingsException;
import pals.base.Storage;
import pals.base.rmi.RMI_Host;
import pals.base.rmi.SSL_Factory;

/**
 * Loads required settings when the context/web-app is started.
 * 
 * @version 1.0
 */
public class PALS_SettingsListener implements ServletContextListener
{
    // Fields ******************************************************************
    private static Settings     settings = null;
    private static SSL_Factory  sfact = null;
    private static RMI_Host[]   hosts = null;
    private static int          hostIndex = -1;
    // Methods *****************************************************************
    /**
     * @see ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
     * 
     * @param sce Refer to interface documentation.
     * 
     * @since 1.0
     */
    @Override
    public void contextInitialized(ServletContextEvent sce)
    {
        try
        {
            System.out.println("Local path: '"+new File("").getCanonicalPath()+"'.");
        }
        catch(IOException ex)
        {
        }
        try
        {
            settings = Settings.load(sce.getServletContext().getRealPath("WEB-INF/web.config"), true);
            // Check we have access to the shared directory
            Storage.StorageAccess access = Storage.checkAccess(settings.getStr("storage/path"), true, true, true, false);
            switch(access)
            {
                case CannotRead:
                    System.err.println("PALS Error: insufficient read access to shared storage!");
                    break;
                case CannotWrite:
                    System.err.println("PALS Error: insufficient write access to shared storage!");
                    break;
                case DoesNotExist:
                    System.err.println("PALS Error: shared storage does not exist!");
                    break;
                default:
                    System.out.println("PALS: successfully loaded settings and checked shared storage path.");
                    break;
            }
            
            // Parse RMI hosts
            {
                String[] hh = settings.getStr("rmi/hosts").trim().split(",");
                ArrayList<RMI_Host> hb = new ArrayList<RMI_Host>();
                String[] t;
                RMI_Host rh;
                for(String h : hh)
                {
                    t = h.trim().split(":");
                    if(t.length == 2)
                    {
                        // Add the host to the buffer
                        try
                        {
                            hb.add(new RMI_Host(null, t[0], Integer.parseInt(t[1])));
                        }
                        catch(NumberFormatException ex)
                        {
                            throw new SettingsException(SettingsException.Type.FailedToParse_InvalidSetting, new IOException("Invalid RMI host '"+h+"'!"));
                        }
                    }
                }
                // Check we have at least one host
                if(hb.isEmpty())
                    throw new SettingsException(SettingsException.Type.FailedToParse_InvalidSetting, new IOException("No RMI hosts have been specified."));
                // Convert buffer to array
                this.hosts = hb.toArray(new RMI_Host[hb.size()]);
            }
            
            // Setup SSL factory, if settings defined
            String  keystorePath = settings.getStr("rmi/keystore/path"),
                    keystorePassword = settings.getStr("rmi/keystore/password");
            
            sfact = SSL_Factory.createFactory(keystorePath, keystorePassword);
            if(sfact == null)
                throw new SettingsException(SettingsException.Type.FailedToParse_InvalidSetting, new IOException("Failed to setup SSL; check path of JKS and password."));
            System.out.println("Loaded PALS settings successfully.");
        }
        catch(SettingsException ex)
        {
            System.err.println("Failed to load PALS settings: '" + ex.getMessage() + "'!");
        }
    }
    @Override
    public void contextDestroyed(ServletContextEvent sce)
    {
        settings = null;
        hosts = null;
        hostIndex = -1;
    }
    // Methods - Static - Accessors ********************************************
    /**
     * Read-only settings for the web-application to interface with
     * the PALS node process.
     * 
     * @return Settings.
     * @since 1.0
     */
    public static Settings getSettings()
    {
        return settings;
    }
    /**
     * The socket factory used for securely connecting with a node.
     * 
     * @return Can be null if the default factory should be used.
     * @since 1.0
     */
    public static SSL_Factory getRMISockFactory()
    {
        return sfact;
    }
    /**
     * Performs round-robin load balancing of the RMI host used.
     * 
     * @return The RMI node to process the web-request; may be null.
     * @since 1.0
     */
    public static synchronized RMI_Host fetchHost()
    {
        // Protection against unloaded or invalid settings
        if(hosts == null)
            return null;
        // Increment counter of host to use
        if(++hostIndex >= hosts.length)
            hostIndex = 0;
        // Return host
        return hosts[hostIndex];
    }
}
