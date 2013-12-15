package pals.base.web;

import java.io.Serializable;

/**
 * Represents a file uploaded from a request.
 */
public class UploadedFile implements Serializable
{
    // Fields ******************************************************************
    private String  name;           // The original name of the file.
    private String  contentType;    // The content-type of the file.
    private long    size;           // The size of the file.
    private String  tempName;       // The temporary filename of the upload.
    // Methods - Constructors **************************************************
    public UploadedFile(String name, String contentType, long size, String tempName)
    {
        this.name = name;
        this.contentType = contentType;
        this.size = size;
        this.tempName = tempName;
    }
    // Methods - Mutators ******************************************************
    /**
     * @param name The name of the file; cannot be null.
     */
    public void setName(String name)
    {
        this.name = name;
    }
    /**
     * @param contentType The content-type of the file.
     */
    public void setContentType(String contentType)
    {
        this.contentType = contentType;
    }
    /**
     * @param size The size of the file, in bytes.
     */
    public void setSize(long size)
    {
        this.size = size;
    }
    /**
     * @param tempName The temporary file-name of the upload.
     */
    public void setTempName(String tempName)
    {
        this.tempName = tempName;
    }
    // Methods - Accessors *****************************************************
    /**
     * @return Retrieves the file-name of the file.
     */
    public String getName()
    {
        return name;
    }
    /**
     * @return Retrieves the content-type of the file; this is not concrete and
     * could be falsified by the user.
     */
    public String getContentType()
    {
        return contentType;
    }
    /**
     * @return The size of the file.
     */
    public long getSize()
    {
        return size;
    }
    /**
     * @return The temporary file-name of the upload.
     */
    public String getTempName()
    {
        return tempName;
    }
}
