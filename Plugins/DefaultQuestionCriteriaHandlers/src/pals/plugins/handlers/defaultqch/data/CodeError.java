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
/**
 * A wrapper for a compiler error.
 */
public class CodeError implements Serializable
{
    static final long serialVersionUID = 1L;
    // Fields *************************************************************8
    private String  className,
                    message;
    private int     line,
                    col;
    // Methods - Constructors **********************************************
    public CodeError()
    {
        this.message = "No error message defined.";
        this.line = -1;
        this.col = -1;
    }
    public CodeError(String className, String message, int line, int col)
    {
        this.className = className;
        this.message = message;
        this.line = line;
        this.col = col;
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The class-name of where the error has occurred.
     */
    public String getClassName()
    {
        return className;
    }
    /**
     * @return The error message from the compiler.
     */
    public String getMessage()
    {
        return message;
    }
    /**
     * @return The line at which the exception occurred.
     */
    public int getLine()
    {
        return line;
    }
    /**
     * @return The column at which the exception occurred.
     */
    public int getCol()
    {
        return col;
    }
}
