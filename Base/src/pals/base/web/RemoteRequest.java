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
package pals.base.web;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * A wrapper used to represent the request data for a web-request, which can be
 * transported over RMI.
 * 
 * Notes:
 * - Fields are get/post fields from a request.
 * - Relative Urls should not start or end with a forward-slash.
 * 
 * Thread-safe.
 * 
 * @version 1.0
 */
public class RemoteRequest implements Serializable
{
    static final long serialVersionUID = 1L;
    // Fields ******************************************************************
    private String                              ipaddr;         // The user's IP address.
    private String                              sessionID;      // The user's session identifier.
    private String                              relativeUrl;    // The relative URL of the request.
    private final HashMap<String,String[]>      fields;         // Any field data provided by the user.
    private final HashMap<String,UploadedFile>  files;          // Any files uploaded by the request.
    // Methods - Constructors **************************************************
    /**
     * Constructs a new instance.
     * 
     * @param sessionID The session ID as a base-64 string.
     * @param relativeUrl The relative URL requested.
     * @param ipaddr The IP address of the request.
     * @throws IllegalArgumentException Thrown if an illegal argument is
     * specified.
     * @since 1.0
     */
    public RemoteRequest(String sessionID, String relativeUrl, String ipaddr) throws IllegalArgumentException
    {
        // Validate data
        if(ipaddr == null)
            throw new IllegalArgumentException("IP address cannot be null!");
        // Set data
        setSessionID(sessionID);
        setRelativeUrl(relativeUrl);
        this.ipaddr = ipaddr;
        this.fields = new HashMap<>();
        this.files = new HashMap<>();
    }
    // Methods - Mutators ******************************************************
    /**
     * Sets the session ID of the user; can be null.
     * 
     * @param sessionID Base64 string.
     * @since 1.0
     */
    public synchronized void setSessionID(String sessionID)
    {
        this.sessionID = sessionID;
    }
    /**
     * Sets the relative URL requested; can be null.
     * 
     * @param relativeUrl The relative URL.
     * @since 1.0
     */
    public synchronized void setRelativeUrl(String relativeUrl)
    {
        if(relativeUrl == null)
            this.relativeUrl = "";
        else
        {
            // Ensure the relative URL does not start with a forward slash
            if(relativeUrl.startsWith("/"))
            {
                relativeUrl = relativeUrl.length() > 1 ? relativeUrl.substring(1) : "";
            }
            // Ensure the relative URL does not end with a forward-slash
            if(relativeUrl.endsWith("/"))
            {
                relativeUrl = relativeUrl.length() > 1 ? relativeUrl.substring(0, relativeUrl.length()-1) : "";
            }
            this.relativeUrl = relativeUrl;
        }
    }
    /**
     * Sets a HTTP POST or GET field.
     * 
     * @param key The key of the field to set.
     * @param value The value of the field; can be null.
     * @since 1.0
     */
    public synchronized void setField(String key, String value)
    {
        this.fields.put(key, new String[]{value});
    }
    /**
     * Sets multiple HTTP POST or GET fields.
     * 
     * @param key The key of the field to set.
     * @param values The values to set for a field; can be null.
     * @since 1.0
     */
    public synchronized void setFields(String key, String[] values)
    {
        this.fields.put(key, values);
    }
    /**
     * Adds a new value to a key. If the key exists, the value is added in a
     * new array. If the key does not exist, the key is mapped to the value.
     * 
     * @param key The key.
     * @param value The value.
     * @since 1.0
     */
    public synchronized void setAddFields(String key, String value)
    {
        if(fields.containsKey(key))
        {
            // Create new array with additional field
            String[] orig = fields.get(key);
            String[] buffer = new String[orig.length+1];
            for(int i = 0; i < orig.length; i++)
                buffer[i] = orig[i];
            buffer[buffer.length-1] = value;
            // Update with new array
            fields.put(key, buffer);
        }
        else
            fields.put(key, new String[]{value});
    }
    /**
     * Sets a file uploaded.
     * 
     * @param key The key/name associated with the file.
     * @param file The file to be stored; can be null.
     * @since 1.0
     */
    public synchronized void setFile(String key, UploadedFile file)
    {
        this.files.put(key, file);
    }
    /**
     * Removes a field.
     * 
     * @param key The key/name of the field to remove.
     * @since 1.0
     */
    public synchronized void removeField(String key)
    {
        this.fields.remove(key);
    }
    /**
     * Removes a file.
     * 
     * @param key The key/name of the file to remove.
     * @since 1.0
     */
    public synchronized void removeFile(String key)
    {
        this.files.remove(key);
    }
    // Methods - Accessors *****************************************************
    /**
     * Indicates if the request contains a field.
     * 
     * @param key The key.
     * @return True = exists, false = does not exist.
     * @since 1.0
     */
    public synchronized boolean containsField(String key)
    {
        return fields.containsKey(key);
    }
    /**
     * Retrieves the IP address.
     * 
     * @return The IP address of the user making the request.
     * @since 1.0
     */
    public synchronized String getIpAddress()
    {
        return ipaddr;
    }
    /**
     * Retrieves the session ID.
     * 
     * @return The session ID provided by the user of the request; can be null.
     * @since 1.0
     */
    public synchronized String getSessionID()
    {
        return sessionID;
    }
    /**
     * Retrieves the relative URL.
     * 
     * @return The requested relative URL; null-protected (becomes
     * empty-string).
     * @since 1.0
     */
    public synchronized String getRelativeUrl()
    {
        return relativeUrl;
    }
    /**
     * Retrieves a field.
     * 
     * @param key The key/name of the field to retrieve.
     * @return The data associated with the key; can be null. Null if the
     * field does not exist. If multiple fields exist, the first is returned.
     * @since 1.0
     */
    public synchronized String getField(String key)
    {
        String[] t = fields.get(key);
        return t != null && t.length > 0 ? t[0] : null;
    }
    /**
     * Retrieves all of the fields.
     * 
     * @param key The key/name of the fields to retrieve.
     * @return All of the field values associated with a key; can be null.
     * @since 1.0
     */
    public synchronized String[] getFields(String key)
    {
        return fields.get(key);
    }
    /**
     * Retrieves all the field names.
     * 
     * @return An array of all the keys of fields.
     * @since 1.0
     */
    public synchronized String[] getFieldNames()
    {
        return fields.keySet().toArray(new String[fields.size()]);
    }
    /**
     * Retrieves the number of fields.
     * 
     * @return The number of fields.
     * @since 1.0
     */
    public synchronized int getFieldsCount()
    {
        return this.fields.size();
    }
    /**
     * Retrieves an uploaded file.
     * 
     * @param key The key/name of the file.
     * @return The file associated with the key; null if the item does not
     * exist.
     * @since 1.0
     */
    public synchronized UploadedFile getFile(String key)
    {
        return this.files.get(key);
    }
    /**
     * Retrieves all of the file-names.
     * 
     * @return An array of all the file-names.
     * @since 1.0
     */
    public synchronized String[] getFileNames()
    {
        return files.keySet().toArray(new String[files.size()]);
    }
    /**
     * Retrieves all of the uploaded files.
     * 
     * @return An array of all the files.
     * @since 1.0
     */
    public synchronized UploadedFile[] getFiles()
    {
        UploadedFile[] files = new UploadedFile[this.files.size()];
        int offset = 0;
        for(Map.Entry<String,UploadedFile> kv : this.files.entrySet())
            files[offset++] = kv.getValue();
        return files;
    }
    /**
     * Retrieves the number of files.
     * 
     * @return Total number of files in the request.
     * @since 1.0
     */
    public synchronized int getFilesCount()
    {
        return this.files.size();
    }
    /**
     * The internal data-structure for storing fields.
     * 
     * @return The map which stores the fields.
     * @since 1.0
     */
    public synchronized Map<String,String[]> getFieldsMap()
    {
        return fields;
    }
    /**
     * The internal data-structure for storing files.
     * 
     * @return The map which stores the files.
     * @since 1.0
     */
    public synchronized Map<String,UploadedFile> getFilesMap()
    {
        return files;
    }
}
