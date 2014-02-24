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
        Method,
        Field
    }                             
    // Fields - Method *********************************************************
    private String                          method;
    private String[]                        methodParameters;
    private String                          methodReturnType;
    // Fields - Field **********************************************************
    private String                          fieldName,
                                            fieldType;
    // Fields - General ********************************************************
    private String                          className;
    private int                             modifiers;
    private int                             markClassOnly;
    private JavaExistsShared.CriteriaType   criteriaType;
    // Methods - Constructors **************************************************
    public ClassExists_Criteria()
    {
        this.className = this.method = this.methodReturnType = null;
        this.methodParameters = new String[0];
        this.fieldName = this.fieldType = null;
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
    public void setMethodParameters(String params)
    {
        this.methodParameters = Misc.arrayStringNonEmpty(params.replace("\r", "").split("\n"));
    }
    /**
     * @param returnType The full class-name of the return-type; can be empty
     * or null.
     */
    public void setMethodReturnType(String returnType)
    {
        this.methodReturnType = returnType;
    }
    /**
     * @param matchType The type of entity being handled.
     */
    public void setCriteriaType(JavaExistsShared.CriteriaType matchType)
    {
        this.criteriaType = matchType;
    }
    /**
     * @param fieldName The name of the field to be checked.
     * @return Indicates if the operation succeeded.
     */
    public boolean setFieldName(String fieldName)
    {
        if(fieldName == null || fieldName.length() == 0)
            return false;
        this.fieldName = fieldName;
        return true;
    }
    /**
     * @param fieldType The type of the field.
     * @return Indicates if the operation succeeded.
     */
    public boolean setFieldType(String fieldType)
    {
        if(fieldType == null || fieldType.length() == 0)
            return false;
        this.fieldType = fieldType;
        return true;
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
    public String[] getMethodParameters()
    {
        return methodParameters;
    }
    /**
     * @return Similar to getParameters, but with values separated by new-line.
     */
    public String getMethodParametersWeb()
    {
        StringBuilder buffer = new StringBuilder();
        for(String p : methodParameters)
            buffer.append(p).append('\n');
        if(buffer.length() > 0)
            buffer.deleteCharAt(buffer.length()-1);
        return buffer.toString();
    }
    /**
     * @return The return-type of the method.
     */
    public String getMethodReturnType()
    {
        return methodReturnType;
    }
    /**
     * @return The name fo the field to check.
     */
    public String getFieldName()
    {
        return fieldName;
    }
    /**
     * @return The type or full class-name of the field.
     */
    public String getFieldType()
    {
        return fieldType;
    }
    /**
     * @return The type of entity being handled.
     */
    public JavaExistsShared.CriteriaType getCriteriaType()
    {
        return criteriaType;
    }
}
