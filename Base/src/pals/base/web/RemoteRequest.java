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
 */
public class RemoteRequest implements Serializable
{
    // Fields ******************************************************************
    private String                              ipaddr;         // The user's IP address.
    private String                              sessionID;      // The user's session identifier.
    private String                              relativeUrl;    // The relative URL of the request.
    private final HashMap<String,String[]>      fields;         // Any field data provided by the user.
    private final HashMap<String,UploadedFile>  files;          // Any files uploaded by the request.
    // Methods - Constructors **************************************************
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
     * @param sessionID Sets the session ID of the user; can be null.
     */
    public synchronized void setSessionID(String sessionID)
    {
        this.sessionID = sessionID;
    }
    /**
     * @param relativeUrl Sets the relative URL requested; can be null.
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
     * @param key The key of the field to set.
     * @param value The value of the field; can be null.
     */
    public synchronized void setField(String key, String value)
    {
        this.fields.put(key, new String[]{value});
    }
    /**
     * @param key The key of the field to set.
     * @param values The values to set for a field; can be null.
     */
    public synchronized void setFields(String key, String[] values)
    {
        this.fields.put(key, values);
    }
    /**
     * @param key The key/name associated with the file.
     * @param file The file to be stored; can be null.
     */
    public synchronized void setFile(String key, UploadedFile file)
    {
        this.files.put(key, file);
    }
    /**
     * @param key The key/name of the field to remove.
     */
    public synchronized void removeField(String key)
    {
        this.fields.remove(key);
    }
    /**
     * @param key The key/name of the file to remove.
     */
    public synchronized void removeFile(String key)
    {
        this.files.remove(key);
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The IP address of the user making the request.
     */
    public synchronized String getIpAddress()
    {
        return ipaddr;
    }
    /**
     * @return The session ID provided by the user of the request; can be null.
     */
    public synchronized String getSessionID()
    {
        return sessionID;
    }
    /**
     * @return The requested relative URL; null-protected (becomes
     * empty-string).
     */
    public synchronized String getRelativeUrl()
    {
        return relativeUrl;
    }
    /**
     * @param key The key/name of the field to retrieve.
     * @return The data associated with the key; can be null. Null if the
     * field does not exist.
     */
    public synchronized String getField(String key)
    {
        String[] t = fields.get(key);
        return t != null && t.length > 0 ? t[0] : null;
    }
    /**
     * @param key The key/name of the fields to retrieve.
     * @return All of the field values associated with a key; can be null.
     */
    public synchronized String[] getFields(String key)
    {
        return fields.get(key);
    }
    /**
     * @return An array of all the keys of fields.
     */
    public synchronized String[] getFieldNames()
    {
        return fields.keySet().toArray(new String[fields.size()]);
    }
    /**
     * @return The number of fields.
     */
    public synchronized int getFieldsCount()
    {
        return this.fields.size();
    }
    /**
     * @param key The key/name of the file.
     * @return The file associated with the key.
     */
    public synchronized UploadedFile getFile(String key)
    {
        return this.files.get(key);
    }
    /**
     * @return An array of all the file-names.
     */
    public synchronized String[] getFileNames()
    {
        return files.keySet().toArray(new String[files.size()]);
    }
    /**
     * @return An array of all the files.
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
     * @return Total number of files in the request.
     */
    public synchronized int getFilesCount()
    {
        return this.files.size();
    }
    /**
     * @return The map which stores the fields.
     */
    public synchronized Map<String,String[]> getFieldsMap()
    {
        return fields;
    }
    /**
     * @return The map which stores the files.
     */
    public synchronized Map<String,UploadedFile> getFilesMap()
    {
        return files;
    }
}
