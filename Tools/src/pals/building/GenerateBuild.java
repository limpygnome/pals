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
package pals.building;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;

/**
 * Generates an automated build of a distributable PALS, with no dependency on
 * a build script/system.
 * 
 * @version 1.0
 */
public class GenerateBuild
{
    /**
     * Entry-point for automated building.
     * 
     * @param args Environment args; ignored.
     * @throws IOException Thrown if an issue occurs copying files.
     * @since 1.0
     */
    public static void main(String[] args) throws IOException
    {
        // Define any file paths here
        final File d = new File("../Builds/"+DateTime.now().toString("YYYY_MM_dd_HH_mm"));
        final File ss = new File("../Shared Storage");
        final File ssDest = new File(d, "shared_storage");
        final File plugins = new File("../Plugins");
        final File pluginsDest = new File(d, "shared_storage/plugins");
        final File node = new File("../Node");
        final File nodeDest = d;
        
        final File toolsDest = new File(d, "_tools");
        final File toolsJavaSandbox = new File("../JavaSandbox/dist");
        final File toolsWindowsUserTool = new File("../WindowsUserTool/WindowsUserTool/bin/Release");
        
        final File web = new File("../Website/build/web");
        final File webDest = new File(d, "web");
        
        final File scripts = new File("../Scripts");
        final File scriptsDest = d;
        
        // Begin ****************************************************************
        // Create dir with date-time as versioning
        if(!d.exists())
            d.mkdirs();
        
        // Copy node files
        FileUtils.copyDirectory(new File(node, "_config_release"), new File(nodeDest, "_config"));
        FileUtils.copyDirectory(new File(node, "_sql"), new File(nodeDest, "_sql"));
        FileUtils.copyDirectory(new File(node, "dist"), nodeDest);
        FileUtils.copyFile(new File(node, "pals.jks"), new File(nodeDest, "pals.jks"));
        
        // Copy tools
        // -- Java Sandbox
        FileUtils.copyDirectory(toolsJavaSandbox, toolsDest);
        // -- Windows User Tool
        FileUtils.copyDirectory(toolsWindowsUserTool, toolsDest);
        
        // Copy shared storage
        FileUtils.copyDirectory(ss, ssDest);
        
        // Copy plugins
        File t;
        for(File p : plugins.listFiles())
        {
            if(p.isDirectory() && (t = new File(p, "dist")).exists())
                FileUtils.copyDirectory(t, new File(pluginsDest, p.getName()));
        }
        
        // Copy website
        FileUtils.copyDirectory(web, webDest);
        
        // Copy scripts
        FileUtils.copyDirectory(scripts, scriptsDest);
        
        // Output success
        System.out.println("Successfully built to:");
        System.out.println(d.getCanonicalPath());
    }
}
