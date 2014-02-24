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
 * A model for holding data for a text-match criteria type.
 */
public class TextMatch_Criteria implements Serializable
{
    static final long serialVersionUID = 1L;
    // Fields ******************************************************************
    private String text;
    private boolean caseSensitive;
    // Methods - Constructors **************************************************
    public TextMatch_Criteria()
    {
        this("unspecified text", false);
    }
    public TextMatch_Criteria(String text, boolean caseSensitive)
    {
        this.text = text;
        this.caseSensitive = false;
    }
    // Methods - Mutators ******************************************************
    /**
     * @param text The exact text to be matched.
     */
    public void setText(String text)
    {
        this.text = text;
    }
    /**
     * @param caseSensitive Indicates if matching should be case-sensitive
     * (true) or insensitive (false).
     */
    public void setCaseSensitive(boolean caseSensitive)
    {
        this.caseSensitive = caseSensitive;
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The text to match.
     */
    public String getText()
    {
        return text;
    }
    /**
     * @return Indicates if the case should be sensitive (true) or
     * insensitive (false).
     */
    public boolean isCaseSensitive()
    {
        return caseSensitive;
    }
}
