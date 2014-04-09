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
package pals.base;

/**
 * Used to store information about a template.
 * 
 * @version 1.0
 */
public class TemplateItem
{
    // Fields ******************************************************************
    private final UUID      plugin;         // The plugin which owns the template.
    private final String    path;           // The path of the template.
    private final String    data;           // The data of the template.
    private final long      lastModified;   // The time at which the template was modified; used by FreeMarker exclusively.
    // Methods - Constructors **************************************************
    /**
     * Constructs a new instance of this class, used for storing data and
     * information about a template.
     * 
     * @param plugin The plugin which owns the template; can be null.
     * @param path The logical path of the template.
     * @param data The template data.
     * @since 1.0
     */
    public TemplateItem(UUID plugin, String path, String data)
    {
        this.plugin = plugin;
        this.path = path;
        this.data = data;
        this.lastModified = System.currentTimeMillis();
    }
    // Methods - Accessors *****************************************************
    /**
     * The plugin which owns the template; can be null.
     * 
     * @return The plugin; can be null.
     * @since 1.0
     */
    public UUID getPluginUUID()
    {
        return plugin;
    }
    /**
     * The logical path of the template. This is the path used to fetch
     * the template from the template manager.
     * 
     * @return The path of the template.
     * @since 1.0
     */
    public String getPath()
    {
        return path;
    }
    /**
     * The render data of the template.
     * 
     * @return The data of the template.
     * @since 1.0
     */
    public String getData()
    {
        return data;
    }
    /**
     * The time the template was last modified. This is used by a third-party
     * template rendering library and is set to the time this instance
     * was constructed.
     * 
     * @return The time at which the template was modified; used by FreeMarker
     * exclusively.
     * @since 1.0
     */
    public long getLastModified()
    {
        return lastModified;
    }
}
