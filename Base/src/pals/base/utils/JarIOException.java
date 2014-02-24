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
package pals.base.utils;

/**
 * Thrown when an exception occurs reading a JAR (Java Archive) file.
 */
public class JarIOException extends Exception
{
    // Enums *******************************************************************
    public enum Reason
    {
        /**
         * Indicates the JAR file was not found.
         */
        FileNotFound,
        /**
         * Indicates the JAR file could not be read due to security/permission issues.
         */
        FileReadPermissions,
        /**
         * Indicates an issued occurred reading the archive.
         */
        FileReadError,
        /**
         * Indicates a specified class could not be found in the archive.
         */
        ClassNotFound,
        /**
         * Indicates a resource in the JAR file could not be read;
         * possibly missing.
         */
        ResourceFetchError
    }
    // Fields ******************************************************************
    private final   String  path;        // The path of the JAR file.
    private final   Reason  reason;      // The reason for the exception.
    // Methods - Constructors **************************************************
    public JarIOException(String path, Reason reason)
    {
        super();
        this.reason = reason;
        this.path = path;
    }
    public JarIOException(String path, Reason reason, String message)
    {
        super(message);
        this.reason = reason;
        this.path = path;
    }
    public JarIOException(String path, Reason reason, Throwable rootException)
    {
        super(rootException);
        this.reason = reason;
        this.path = path;
    }
    public JarIOException(String path, Reason reason, String message, Throwable rootException)
    {
        super(message, rootException);
        this.reason = reason;
        this.path = path;
    }
    // Methods - Accessors *****************************************************
    /**
     * The path of the JAR file.
     * @return System path; may be invalid.
     */
    public String getPath()
    {
        return path;
    }
    /**
     * The reason for the exception.
     * @return Reason.
     */
    public Reason getReason()
    {
        return reason;
    }
}
