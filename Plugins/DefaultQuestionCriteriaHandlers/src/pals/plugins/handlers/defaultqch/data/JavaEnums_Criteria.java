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
 * Stores the criteria settings for checking enums.
 * 
 * @version 1.0
 */
public class JavaEnums_Criteria implements Serializable
{
    static final long serialVersionUID = 1L;
    
    // Fields ******************************************************************
    private String      className;
    private String[]    values;
    private boolean     caseSensitive,
                        allowExtraValues;
    // Methods - Constructors **************************************************
    /**
     * Constructs a new instance of this criteria.
     * 
     * @since 1.0
     */
    public JavaEnums_Criteria()
    {
        this.className = null;
        this.values = new String[0];
        this.caseSensitive = this.allowExtraValues = false;
    }
    // Methods - Mutators ******************************************************
    /**
     * Sets the class-name of the enum.
     * 
     * @param className Full class-name.
     * @return Indicates the success of the operation.
     * @since 1.0
     */
    public boolean setClassName(String className)
    {
        if(className == null || className.length() == 0)
            return false;
        this.className = className;
        return true;
    }
    /**
     * Sets the String values of the enum.
     * 
     * @param values String values.
     * @return Indicates the success of the operation.
     * @since 1.0
     */
    public boolean setValues(String[] values)
    {
        if(values == null)
            return false;
        values = Misc.arrayStringUnique(values);
        if(values.length == 0)
            return false;
        this.values = values;
        return true;
    }
    /**
     * Sets the case sensitivity of value comparison.
     * 
     * @param caseSensitive True = case sensitive comparison, false = not
     * case sensitive.
     * @since 1.0
     */
    public void setCaseSensitive(boolean caseSensitive)
    {
        this.caseSensitive = caseSensitive;
    }
    /**
     * Sets if to allow extra values.
     * 
     * @param allowExtraValues True = allow, false = disallow.
     * @since 1.0
     */
    public void setAllowExtraValues(boolean allowExtraValues)
    {
        this.allowExtraValues = allowExtraValues;
    }
    // Methods - Accessors *****************************************************
    /**
     * The class-name of the enum.
     * 
     * @return Full class-name.
     * @since 1.0
     */
    public String getClassName()
    {
        return className;
    }
    /**
     * Retrieves the values the enum should contain.
     * 
     * @return The array of values the target enum should contain; can be
     * empty.
     */
    public String[] getValues()
    {
        return values;
    }
    /**
     * Fetches the values in a web-format.
     * 
     * @return Each value is separated by a new-line character.
     * @since 1.0
     */
    public String getValuesWeb()
    {
        StringBuilder sb = new StringBuilder();
        for(String s : values)
            sb.append(s).append("\n");
        if(sb.length()>0)
            sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }
    /**
     * Indicates if value comparison should be case-sensitive.
     * 
     * @return True = case sensitive, false = not case sensitive.
     * @since 1.0
     */
    public boolean isCaseSensitive()
    {
        return caseSensitive;
    }
    /**
     * Indicates if to allow extra values. If an extra value is present, it
     * is treated as if an answer value is missing.
     * 
     * @return True = allow extra, false = not allow extra.
     * @since 1.0
     */
    public boolean isAllowExtraValues()
    {
        return allowExtraValues;
    }
}
