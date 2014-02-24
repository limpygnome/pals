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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A template loader for the FreeMarker template engine. This is similar to
 * StringTemplateLoader:
 * http://freemarker.org/docs/api/freemarker/cache/StringTemplateLoader.html
 * 
 * Except additional information, relevant to PALS, is stored.
 * 
 * Thread-safe.
 */
public class TemplateLoader implements freemarker.cache.TemplateLoader
{
    // Fields ******************************************************************
    private final HashMap<String,TemplateItem> items;   // Stores the templates.
    // Methods - Constructors **************************************************
    public TemplateLoader()
    {
        this.items = new HashMap<>();
    }
    // Methods *****************************************************************
    /**
     * @param path The path to check.
     * @return Indicates if the path exists (true) or does not exist (false).
     */
    public synchronized boolean contains(String path)
    {
        return items.containsKey(path);
    }
    /**
     * Sets a template in the collection.
     * 
     * @param plugin The owner of the template.
     * @param path The path of the template.
     * @param data The template data/source.
     */
    public synchronized void put(Plugin plugin, String path, String data)
    {
        items.put(path, new TemplateItem(plugin != null ? plugin.getUUID() : null, path, data));
    }
    /**
     * @param path The path of the template to be removed.
     */
    public synchronized void remove(String path)
    {
        items.remove(path);
    }
    /**
     * @param plugin Any templates belonging to the specified plugin are
     * removed.
     */
    public synchronized void remove(Plugin plugin)
    {
        UUID pluginUuid = plugin == null ? null : plugin.getUUID();
        // Locate all the templates owned by a plugin and remove them
        Iterator<Map.Entry<String,TemplateItem>> it = items.entrySet().iterator();
        Map.Entry<String,TemplateItem> item;
        UUID uuid;
        while(it.hasNext())
        {
            item = it.next();
            uuid = item.getValue().getPluginUUID();
            if(uuid == pluginUuid || (uuid != null && uuid.equals(pluginUuid)))
                it.remove();
        }
    }
    /**
     * Removes all of the items in the collection. You must also clear the
     * template cache in the FreeMaker Configuration class.
     */
    public synchronized void clear()
    {
        items.clear();
    }
    // Methods - Interface Overrides *******************************************
    /**
     * Refer to (FreeMarker) TemplateLoader for documentation.
     * 
     * Used by FreeMarker to close a template source.
     */
    @Override
    public synchronized void closeTemplateSource(Object o) throws IOException
    {
        // No need to do anything...
    }
    /**
     * Refer to (FreeMarker) TemplateLoader for documentation.
     * 
     * Used by FreeMarker to get a source for interfacing with a template.
     */
    @Override
    public synchronized Object findTemplateSource(String path) throws IOException
    {
        return items.get(path);
    }
    /**
     * Refer to (FreeMarker) TemplateLoader for documentation.
     * 
     * Used by FreeMarker to read a template.
     */
    @Override
    public synchronized Reader getReader(Object templateSource, String encoding) throws IOException
    {
        return new StringReader(((TemplateItem)templateSource).getData());
    }
    /**
     * Refer to (FreeMarker) TemplateLoader for documentation.
     * 
     * Indicates when the template was last modified.
     */
    @Override
    public synchronized long getLastModified(Object templateSource)
    {
        return ((TemplateItem)templateSource).getLastModified();
    }
    // Methods - Accessors *****************************************************
    /**
     * @param path The path of the template to retrieve.
     * @return The template at the specified path or null.
     */
    public synchronized TemplateItem getItem(String path)
    {
        return items.get(path);
    }
    /**
     * @return All of the templates in the collection.
     */
    public synchronized TemplateItem[] getItems()
    {
        return items.values().toArray(new TemplateItem[items.size()]);
    }
    /**
     * @return The underlying map used to store templates.
     */
    public synchronized HashMap<String,TemplateItem> getMap()
    {
        return items;
    }
}
