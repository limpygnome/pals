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

/**
 * Stores the criteria settings for Java Custom criteria.
 * 
 * @version 1.0
 */
public class JavaCustom_Criteria implements Serializable
{
    static final long serialVersionUID = 1L;
    
    // Fields ******************************************************************
    private String                  className,
                                    method;
    private int                     messageThreshold;
    // Methods - Constructors **************************************************
    /**
     * Creates a new instance.
     * 
     * @since 1.0
     */
    public JavaCustom_Criteria()
    {
        this.className = null;
        this.method = null;
        this.messageThreshold = 30;
    }
    // Methods - Mutators ******************************************************
    /**
     * Sets the entry-point class-name.
     * 
     * @param className The full class-name.
     * @since 1.0
     * @return True = updated, false = invalid.
     */
    public boolean setClassName(String className)
    {
        if(className == null || className.length() < 0)
            return false;
        this.className = className;
        return true;
    }
    /**
     * Sets the entry-point method name.
     * 
     * @param method The method name.
     * @since 1.0
     * @return True = updated, false = invalid.
     */
    public boolean setMethod(String method)
    {
        if(className == null || className.length() < 0)
            return false;
        this.method = method;
        return true;
    }
    /**
     * Sets the maximum number of feedback messages allowed.
     * 
     * @param messageThreshold Must be 1 or greater.
     * @since 1.0
     */
    public void setMessageThreshold(int messageThreshold)
    {
        this.messageThreshold = messageThreshold;
    }
    // Methods - Accessors *****************************************************
    /**
     * Retrieves the entry-point class-name.
     * 
     * @return Full class-name.
     * @since 1.0
     */
    public String getClassName()
    {
        return className;
    }
    /**
     * Retrieves the entry-point method.
     * 
     * @return Method name.
     * @since 1.0
     */
    public String getMethod()
    {
        return method;
    }
    /**
     * The maximum allowed feedback messages. Once exceeded, the process is
     * terminated.
     * 
     * @return Maximum number of feedback messages.
     * @since 1.0
     */
    public int getMessageThreshold()
    {
        return messageThreshold;
    }
}
