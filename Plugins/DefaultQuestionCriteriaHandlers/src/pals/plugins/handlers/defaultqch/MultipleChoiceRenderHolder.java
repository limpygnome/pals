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
package pals.plugins.handlers.defaultqch;

/**
 * A simple class for the template engine to render the possible choices of a
 * multiple choice question.
 */
public class MultipleChoiceRenderHolder
{
    // Fields ******************************************************************
    private final int           number;
    private final String        text;
    private final boolean       selected;
    // Methods - Constructors **************************************************
    public MultipleChoiceRenderHolder(int number, String text, boolean selected)
    {
        this.number = number;
        this.text = text;
        this.selected = selected;
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The random-index of the choice.
     */
    public int getNumber()
    {
        return number;
    }
    /**
     * @return Indicates if the choice is currently selected.
     */
    public boolean isSelected()
    {
        return selected;
    }
    /**
     * @return The text of the choice.
     */
    public String getText()
    {
        return text;
    }
}