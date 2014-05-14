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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.FileUtils;

/**
 * Deploys the latest build to a cluster. This is based on the plugins being
 * stored in the shared storage., with all files, other than node
 * configuration, replaceable. Old files are not deleted, unless initially
 * deployed from here.
 */
public class DeployCluster
{
    // Enums *******************************************************************
    public enum ArgMode
    {
        NONE,
        NODES,
        PLUGINS
    }
    // Constants ***************************************************************
    private static final String DIR_BUILD = "../Builds";
    private static final String DIR_BUILD_VALID_FILECHECK = "_config/node.config";
    private static final String DIR_BUILD_PLUGINS = "shared_storage/plugins";
    // Methods - Entry Point ***************************************************
    public static void main(String[] args)
    {
        try
        {
            ArrayList<File> nodes = new ArrayList<>();
            File plugins = null;
            // Parse arguments
            if(args.length == 0)
            {
                System.out.println("Expected args:");
                System.out.println("- The file paths of nodes: nodes,<node file path>,...");
                System.out.println("Optional args:");
                System.out.println("- The path of plugins to be updated (global): plugins,<path>");
                return;
            }
            ArgMode mode = ArgMode.NONE;
            File t;
            for(String s : args)
            {
                // Check for mode switch
                switch(s)
                {
                    case "nodes":
                        mode = ArgMode.NODES;
                        break;
                    case "plugins":
                        mode = ArgMode.PLUGINS;
                        break;
                    default:
                        switch(mode)
                        {
                            case NODES:
                                // Parse new node dest path
                                t = new File(s);
                                if(!t.exists())
                                {
                                    System.err.println("Node path '"+t.getCanonicalPath()+"' not available.");
                                    return;
                                }
                                else
                                {
                                    nodes.add(t);
                                    System.out.println("Added path '"+t.getCanonicalPath()+"'.");
                                }
                                break;
                            case PLUGINS:
                                if(!(plugins = new File(s)).exists())
                                {
                                    System.err.println("Plugins path '"+plugins.getCanonicalPath()+"' does not exist.");
                                    return;
                                }
                                break;
                        }
                        break;
                }
            }
            // Check we have nodes
            if(nodes.isEmpty())
            {
                System.err.println("No node destinations specified.");
                return;
            }
            // Locate latest build dir
            File src = null;
            for(File d : new File(DIR_BUILD).listFiles())
            {
                if(d.isDirectory() && (new File(d, DIR_BUILD_VALID_FILECHECK).exists()) && (src == null || (src.getName().compareTo(d.getName()) < 0)))
                    src = d;
            }
            // Check we have a valid build dir
            if(src == null)
            {
                System.err.println("Could not find build folder at '"+new File(DIR_BUILD).getCanonicalPath()+"'.");
                return;
            }
            else
                System.out.println("Using build folder at '"+src.getCanonicalPath()+"'.");
            // Carry out procedure for each node
            final String[] excluded = new String[]{"_config","shared","shared_storage"};
            for(File n : nodes)
            {
                System.out.println("Updating node at '"+n.getCanonicalPath()+"'...");
                if(!updatePath(src, n, excluded))
                    System.err.println("Failed to update node at '"+n.getCanonicalPath()+"'.");
                else
                    System.out.println("Node updated.");
            }
            // Update nodes dir, if specified
            if(plugins == null)
                System.out.println("No plugins dir specified, not updated.");
            else
            {
                System.out.println("Updating plugins at '"+plugins.getCanonicalPath()+"'...");
                // Fetch src
                File srcPlugins = new File(src, DIR_BUILD_PLUGINS);
                if(!src.exists())
                    System.err.println("Could not locate plugins dir '"+DIR_BUILD_PLUGINS+"' in build folder.");
                else
                {
                    if(updatePath(srcPlugins, plugins, new String[]{}))
                        System.out.println("Updated plugins.");
                    else
                        System.err.println("Failed to update plugins at '"+plugins.getCanonicalPath()+"'.");
                }
            }
        }
        catch(IOException ex)
        {
            ex.printStackTrace(System.err);
        }
    }
    // Methods - Static ********************************************************
    private static boolean updatePath(File src, File dest, String[] excludedTopLevelFiles) throws IOException
    {
        final List<String> excluded = Arrays.asList(excludedTopLevelFiles);
        // Delete all files, except those above
        for(File f : dest.listFiles())
        {
            // Check item is not excluded
            if(!excluded.contains(f.getName()))
            {
                // Delete
                if(f.isDirectory())
                    FileUtils.deleteDirectory(f);
                else if(f.isFile())
                    f.delete();
            }
        }
        // Copy new files
        for(File f : src.listFiles())
        {
            // Check item is not excluded
            if(!excluded.contains(f.getName()))
            {
                // Delete
                if(f.isDirectory())
                    FileUtils.copyDirectory(f, new File(dest, f.getName()));
                else if(f.isFile())
                    FileUtils.copyFile(f, new File(dest, f.getName()));
            }
        }
        return true;
    }
}
