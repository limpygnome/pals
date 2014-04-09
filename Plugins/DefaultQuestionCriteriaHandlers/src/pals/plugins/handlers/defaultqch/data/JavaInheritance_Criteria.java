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
package pals.plugins.handlers.defaultqch.data;

import java.io.Serializable;
import pals.base.utils.Misc;

/**
 * Used for storing settings for Java inheritance criterias.
 * 
 * @version 1.0
 */
public class JavaInheritance_Criteria implements Serializable
{
    static final long serialVersionUID = 1L;
    
    // Fields ******************************************************************
    private String      className,
                        inheritedClassName,
                        inheritedGenericType;
    private String[]    interfaces;
    // Methods - Constructors **************************************************
    /**
     * Constructs a new instance.
     * 
     * @since 1.0
     */
    public JavaInheritance_Criteria()
    {
        this.className = inheritedClassName = inheritedGenericType = null;
        this.interfaces = new String[0];
    }
    // Methods - Mutators ******************************************************
    /**
     * Sets the class-name of the class being targeted for checking.
     * 
     * @param className The full class-name.
     * @since 1.0
     */
    public void setClassName(String className)
    {
        this.className = className;
    }
    /**
     * Sets the class-name of the class being inherited by the target.
     * 
     * @param inheritedClassName The full class-name.
     * @since 1.0
     */
    public void setInheritedClassName(String inheritedClassName)
    {
        this.inheritedClassName = inheritedClassName != null && inheritedClassName.length() > 0 ? inheritedClassName : null;
    }
    /**
     * Sets the class-name of the generic-type of the class being inherited by
     * the target.
     * 
     * @param inheritedGenericType The full class-name.
     * @since 1.0
     */
    public void setInheritedGenericType(String inheritedGenericType)
    {
        this.inheritedGenericType = inheritedGenericType != null && inheritedGenericType.length() > 0 ? inheritedGenericType : null;
    }
    /**
     * Sets the interfaces the target-class must use.
     * 
     * @param interfaces Array of full class-names of the interfaces.
     * @since 1.0
     */
    public void setInterfaces(String[] interfaces)
    {
        this.interfaces = interfaces == null ? new String[0] : Misc.arrayStringUnique(interfaces);
    }
    // Methods - Accessors *****************************************************
    /**
     * The class being targeted.
     * 
     * @return The full class-name.
     * @since 1.0
     */
    public String getClassName()
    {
        return className;
    }
    /**
     * The inherited class of the target.
     * 
     * @return The full class-name.
     * @since 1.0
     */
    public String getInheritedClassName()
    {
        return inheritedClassName;
    }
    /**
     * Indicates if to check the inherited class of the target.
     * 
     * @return True = check extended class, false = do not check.
     * @since 1.0
     */
    public boolean isInheritedClassNameConsidered()
    {
        return inheritedClassName != null;
    }
    /**
     * The generic class of the inherited class.
     * 
     * @return The full class-name.
     * @since 1.0
     */
    public String getInheritedGenericType()
    {
        return inheritedGenericType;
    }
    /**
     * Indicates if to check the generic type of the target.
     * 
     * @return True = check generic type, false = do not check.
     * @since 1.0
     */
    public boolean isInheritedGenericTypeConsidered()
    {
        return inheritedGenericType != null;
    }
    /**
     * Retrieves the full-name for the extended class-name, which is either
     * the class-name or the class-name with the generic-type. This is
     * intended for display purposes.
     * 
     * @return Full extended class-name.
     * @since 1.0
     */
    public String getFullExtendedClassName()
    {
        return inheritedGenericType != null ? inheritedClassName + "<"+inheritedGenericType+">" : inheritedClassName;
    }
    /**
     * Fetches the interfaces the target class must extend.
     * 
     * @return The full class-names in array, can be empty.
     * @since 1.0
     */
    public String[] getInterfaces()
    {
        return interfaces;
    }
    /**
     * Indicates if to check implemented interfaces.
     * 
     * @return True = check implemented interfaces, false = do not check.
     * @since 1.0
     */
    public boolean isInterfaces()
    {
        return interfaces.length > 0;
    }
    /**
     * Fetches the list of interfaces in a web format, for display on a form.
     * 
     * @return Each interface, separated by new-line character.
     * @since 1.0
     */
    public String getInterfacesWeb()
    {
        StringBuilder sb = new StringBuilder();
        for(String i : interfaces)
            sb.append(i).append("\n");
        if(sb.length() > 0)
            sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }
}
