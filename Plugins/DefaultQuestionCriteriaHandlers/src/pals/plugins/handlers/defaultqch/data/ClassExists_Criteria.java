package pals.plugins.handlers.defaultqch.data;

import java.io.Serializable;

/**
 * Contains data for a class-exists criteria.
 */
public class ClassExists_Criteria implements Serializable
{
    static final long serialVersionUID = 1L;
    // Fields ******************************************************************
    private String className;
    // Methods - Constructors **************************************************
    public ClassExists_Criteria()
    {
        this.className = null;
    }
    // Methods - Mutators ******************************************************
    /**
     * @param className Sets the class-name to match; can be null.
     */
    public void setClassName(String className)
    {
        this.className = className;
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The class-name to check exists; this can be null.
     */
    public String getClassName()
    {
        return className;
    }
}
