package pals.plugins.handlers.defaultqch.java;

/**
 *
 * @author limpygnome
 */
public class WhitelistException extends ClassNotFoundException
{
    private String className;
    
    public WhitelistException(String className)
    {
        this.className = className;
    }
    
    public String getClassName()
    {
        return className;
    }
}
