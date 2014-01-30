package pals.plugins.handlers.defaultqch.data;

import java.io.Serializable;
import pals.base.utils.Misc;
import pals.plugins.handlers.defaultqch.criterias.JavaExistsShared;

/**
 * Contains data for a class-exists criteria.
 * 
 * Refer to the following API for setting the modifiers:
 * http://docs.oracle.com/javase/7/docs/api/java/lang/reflect/Modifier.html
 */
public class ClassExists_Criteria implements Serializable
{
    static final long serialVersionUID = 1L;
    // Enums *******************************************************************
    public enum MatchType
    {
        Class,
        Method
    }
    // Fields ******************************************************************
    private String                          className,
                                            method;
    private String[]                        parameters;
    private String                          returnType;
    private int                             modifiers;
    private int                             markClassOnly;
    private JavaExistsShared.CriteriaType   criteriaType;
    // Methods - Constructors **************************************************
    public ClassExists_Criteria()
    {
        this.className = this.method = this.returnType = null;
        this.parameters = new String[0];
        this.modifiers = 0;
        this.markClassOnly = 50;
        this.criteriaType = JavaExistsShared.CriteriaType.Class;
    }
    // Methods - Mutators ******************************************************
    /**
     * @param className Sets the class-name to match; can be null.
     * @return Indicates if the specified value is valid.
     */
    public boolean setClassName(String className)
    {
        if(className == null || className.length() == 0)
            return false;
        this.className = className;
        return true;
    }
    /**
     * @param method Sets the method to check exists.
     * @return Indicates if the specified value is valid.
     */
    public boolean setMethod(String method)
    {
        if(method == null || method.length() == 0)
            return false;
        this.method = method;
        return true;
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
    /**
     * @param params Sets the parameters for the method being testedl this
     * should be classes separated by the new-line character.
     */
    public void setParameters(String params)
    {
        this.parameters = Misc.arrayStringNonEmpty(params.replace("\r", "").split("\n"));
    }
    /**
     * @param returnType The full class-name of the return-type.
     */
    public void setReturnType(String returnType)
    {
        this.returnType = returnType;
    }
    /**
     * @param matchType The type of entity being handled.
     */
    public void setCriteriaType(JavaExistsShared.CriteriaType matchType)
    {
        this.criteriaType = matchType;
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
     * @return The method to check exists; this can be null.
     */
    public String getMethod()
    {
        return method;
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
    /**
     * @return The full class-names of parameters, in-order, of the method
     * to match.
     */
    public String[] getParameters()
    {
        return parameters;
    }
    /**
     * @return Similar to getParameters, but with values separated by new-line.
     */
    public String getParametersWeb()
    {
        StringBuilder buffer = new StringBuilder();
        for(String p : parameters)
            buffer.append(p).append('\n');
        if(buffer.length() > 0)
            buffer.deleteCharAt(buffer.length()-1);
        return buffer.toString();
    }
    /**
     * @return The return-type of the method.
     */
    public String getReturnType()
    {
        return returnType;
    }
    /**
     * @return The type of entity being handled.
     */
    public JavaExistsShared.CriteriaType getCriteriaType()
    {
        return criteriaType;
    }
}
