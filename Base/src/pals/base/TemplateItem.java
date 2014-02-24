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
package pals.base;

/**
 * Used to store information about a template.
 */
public class TemplateItem
{
    // Fields ******************************************************************
    private final UUID      plugin;         // The plugin which owns the template.
    private final String    path;           // The path of the template.
    private final String    data;           // The data of the template.
    private final long      lastModified;   // The time at which the template was modified; used by FreeMarker exclusively.
    // Methods - Constructors **************************************************
    public TemplateItem(UUID plugin, String path, String data)
    {
        this.plugin = plugin;
        this.path = path;
        this.data = data;
        this.lastModified = System.currentTimeMillis();
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The plugin which owns the template; can be null.
     */
    public UUID getPluginUUID()
    {
        return plugin;
    }
    /**
     * @return The path of the template.
     */
    public String getPath()
    {
        return path;
    }
    /**
     * @return The data of the template.
     */
    public String getData()
    {
        return data;
    }
    /**
     * @return The time at which the template was modified; used by FreeMarker
     * exclusively.
     */
    public long getLastModified()
    {
        return lastModified;
    }
}
