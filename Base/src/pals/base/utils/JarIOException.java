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

/**
 * Thrown when an exception occurs reading a JAR (Java Archive) file.
 * 
 * @version 1.0
 */
public class JarIOException extends Exception
{
    // Enums *******************************************************************
    /**
     * The reason for the exception.
     * 
     * @since 1.0
     */
    public enum Reason
    {
        /**
         * Indicates the JAR file was not found.
         * 
         * @since 1.0
         */
        FileNotFound,
        /**
         * Indicates the JAR file could not be read due to security/permission issues.
         * 
         * @since 1.0
         */
        FileReadPermissions,
        /**
         * Indicates an issued occurred reading the archive.
         * 
         * @since 1.0
         */
        FileReadError,
        /**
         * Indicates a specified class could not be found in the archive.
         * 
         * @since 1.0
         */
        ClassNotFound,
        /**
         * Indicates a resource in the JAR file could not be read;
         * possibly missing.
         * 
         * @since 1.0
         */
        ResourceFetchError
    }
    // Fields ******************************************************************
    private final   String  path;        // The path of the JAR file.
    private final   Reason  reason;      // The reason for the exception.
    // Methods - Constructors **************************************************
    /**
     * Constructs a new instance.
     * 
     * @param path The path of the JAR.
     * @param reason The reason for the exception.
     * @since 1.0
     */
    public JarIOException(String path, Reason reason)
    {
        super();
        this.reason = reason;
        this.path = path;
    }
    /**
     * Constructs a new instance.
     * 
     * @param path The path of the JAR.
     * @param reason The reason for the exception.
     * @param message An exception message.
     * @since 1.0
     */
    public JarIOException(String path, Reason reason, String message)
    {
        super(message);
        this.reason = reason;
        this.path = path;
    }
    /**
     * Constructs a new instance.
     * 
     * @param path The path of the JAR.
     * @param reason The reason for the exception.
     * @param rootException The underlying exception.
     * @since 1.0
     */
    public JarIOException(String path, Reason reason, Throwable rootException)
    {
        super(rootException);
        this.reason = reason;
        this.path = path;
    }
    /**
     * Constructs a new instance.
     * 
     * @param path The path of the JAR.
     * @param reason The reason for the exception.
     * @param message An exception message.
     * @param rootException The underlying exception.
     * @since 1.0
     */
    public JarIOException(String path, Reason reason, String message, Throwable rootException)
    {
        super(message, rootException);
        this.reason = reason;
        this.path = path;
    }
    // Methods - Accessors *****************************************************
    /**
     * The path of the JAR file.
     * 
     * @return System path; may be invalid.
     * @since 1.0
     */
    public String getPath()
    {
        return path;
    }
    /**
     * The reason for the exception.
     * 
     * @return The {@link Reason}.
     * @since 1.0
     */
    public Reason getReason()
    {
        return reason;
    }
}
