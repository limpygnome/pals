package pals.plugins.handlers.defaultqch.data;

import java.io.Serializable;

/**
 * Contains data for a class-exists criteria.
 * 
 * Refer to the following API for setting the modifiers:
 * http://docs.oracle.com/javase/7/docs/api/java/lang/reflect/Modifier.html
 */
public class ClassExists_Criteria implements Serializable
{
    static final long serialVersionUID = 1L;
    // Fields ******************************************************************
    private String  className;
    private int     modifiers;
    private int     markClassOnly;
    // Methods - Constructors **************************************************
    public ClassExists_Criteria()
    {
        this.className = null;
        this.modifiers = 0;
        this.markClassOnly = 50;
    }
    // Methods - Mutators ******************************************************
    /**
     * @param className Sets the class-name to match; can be null.
     */
    public void setClassName(String className)
    {
        this.className = className;
    }
    /**
     * @param modifiers The modifiers to match.
     */
    public void setModifiers(int modifiers)
    {
        this.modifiers = modifiers;
    }
    /**
     * @param markClassOnly The mark if only the class is found.
     */
    public void setMarkClassOnly(int markClassOnly)
    {
        this.markClassOnly = markClassOnly;
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The class-name to check exists; this can be null.
     */
    public String getClassName()
    {
        return className;
    }
    /**
     * @return The modifiers to be matched.
     */
    public int getModifiers()
    {
        return modifiers;
    }
    /**
     * @return The mark if only the class is found.
     */
    public int getMarkClassOnly()
    {
        return markClassOnly;
    }
}
