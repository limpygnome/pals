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
import java.util.ArrayList;

/**
 * Holds data for a multiple-choice criteria.
 */
public class MultipleChoice_Criteria implements Serializable
{
    static final long serialVersionUID = 1L;
    // Fields ******************************************************************
    private Integer[] indexesCorrect;
    // Methods - Constructors **************************************************
    public MultipleChoice_Criteria()
    {
        this.indexesCorrect = new Integer[0];
    }
    public MultipleChoice_Criteria(Integer[] values)
    {
        this.indexesCorrect = values;
    }
    // Methods - Mutators ******************************************************
    /**
     * @param indexesCorrect The indexes of the multiple choice answers which
     * are correct.
     */
    public void setIndexesCorrect(Integer[] indexesCorrect)
    {
        this.indexesCorrect = indexesCorrect;
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The indexes of the choices which are correct.
     */
    public Integer[] getIndexesCorrect()
    {
        return indexesCorrect;
    }
    /**
     * @param qdata Question data.
     * @return Correct answers as text; can be empty.
     */
    public String[] getCorrect(MultipleChoice_Question qdata)
    {
        ArrayList<String> buffer = new ArrayList<>();
        String[] text = qdata.getAnswers();
        for(Integer index : indexesCorrect)
        {
            if(index >= 0 && index < text.length)
                buffer.add(text[index]);
        }
        return buffer.toArray(new String[buffer.size()]);
    }
}
