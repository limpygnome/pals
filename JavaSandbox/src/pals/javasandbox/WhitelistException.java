package pals.javasandbox;

/**
 * An exception thrown when a prohibited/non-whitelisted class has been
 * attempted to be loaded into the JVM from a class-loader.
 */
public class WhitelistException extends ClassNotFoundException
{
    // Fields ******************************************************************
    private String className;
    // Methods - Constructors **************************************************
    public WhitelistException(String className)
    {
        this.className = className;
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The class attempted to be loaded during runtime.
     */
    public String getClassName()
    {
        return className;
    }
}
