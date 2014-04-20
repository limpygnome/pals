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
package pals.base.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import pals.base.NodeCore;
import pals.base.Plugin;

/**
 * An extension of {@link ObjectInputStream} to resolve classes available from
 * plugin JARs, which have not been loaded into the runtime of the base
 * library.
 * 
 * @version 1.0
 */
public class PluginObjectInputStream extends ObjectInputStream
{
    // Fields ******************************************************************
    private NodeCore core;
    // Methods - Constructors **************************************************
    /**
     * Constructs a new instance.
     * 
     * @param core Current instance of core.
     * @param is The input-stream of the data.
     * @throws IOException Thrown by {@link ObjectInputStream}.
     * @since 1.0
     */
    public PluginObjectInputStream(NodeCore core, InputStream is) throws IOException
    {
        super(is);
        this.core = core;
    }
    /**
     * Reads a class from the input-stream.
     * 
     * @param osc The class to be located.
     * @return The class located.
     * @throws IOException Thrown if an I/O error occurs reading the data.
     * @throws ClassNotFoundException Thrown if the class cannot be found.
     * @since 1.0
     */
    @Override
    protected Class<?> resolveClass(ObjectStreamClass osc) throws IOException, ClassNotFoundException
    {
        String className = osc.getName();
        // Remove [L<class name>; for indicating a class for an array
        //if(className.startsWith("[L") && className.endsWith(";") && className.length() > 3)
        //    className = className.substring(2, className.length()-1);
        // Attempt to fetch from super first
        try
        {
            return super.resolveClass(osc);
        }
        catch(ClassNotFoundException ex)
        {
        }
        // Iterate each plugin for the class
        for(Plugin p : core.getPlugins().getPlugins())
        {
            try
            {
                return Class.forName(className, false, p.getJarIO().getRawLoader());
            }
            catch(ClassNotFoundException ex)
            {
            }
        }
        throw new ClassNotFoundException("PluginObjectInputStream - cannot find class '"+className+"'");
    }
}
