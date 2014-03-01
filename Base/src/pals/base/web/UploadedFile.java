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
package pals.base.web;

import java.io.Serializable;

/**
 * Represents a file uploaded from a request.
 */
public class UploadedFile implements Serializable
{
    static final long serialVersionUID = 1L;
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
