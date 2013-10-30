/*
    PALS - Programming and Assessment Learning System
    ****************************************************************************
    File:       pals.base.utils.JarIOException
    License:    Creative Commons Attribution-ShareAlike 3.0 Unported
                CC BY-SA 3.0
                http://creativecommons.org/licenses/by-sa/3.0
    ****************************************************************************
    Author(s):  Marcus Craske       marcus.craske@uea.ac.uk
    ****************************************************************************
    Change-Log:
                2014-xx-xx  Finalized.
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
