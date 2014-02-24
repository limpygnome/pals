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

/**
 * Stores the data for a code-java question.
 */
public class CodeJava_Question implements Serializable
{
    static final long serialVersionUID = 1L;
    // Enums *******************************************************************
    public enum QuestionType
    {
        Fragment(0),
        Upload(1);
        private final int formValue;
        private QuestionType(int formValue)
        {
            this.formValue = formValue;
        }
        public int getFormValue()
        {
            return formValue;
        }
        public static QuestionType parse(String value)
        {
            if(value == null || value.length() == 0)
                return Fragment;
            try
            {
                switch(Integer.parseInt(value))
                {
                    case 0:
                        return Fragment;
                    case 1:
                        return Upload;
                }
            }
            catch(NumberFormatException ex) {}
            return Fragment;
        }
    }
    // Fields ******************************************************************
    private String          text;
    private String          skeleton;
    private String[]        whitelist;
    private QuestionType    type;
    // Methods - Constructors **************************************************
    public CodeJava_Question()
    {
        this.text = "Undefined question text...";
        this.skeleton = "";
        this.whitelist = new String[0];
        this.type = QuestionType.Fragment;
    }

    // Methods - Mutators ******************************************************
    /**
     * @param text Sets the question text.
     */
    public void setText(String text)
    {
        this.text = text;
    }
    /**
     * @param skeleton Sets the skeleton code.
     */
    public void setSkeleton(String skeleton)
    {
        this.skeleton = skeleton;
    }
    /**
     * @param whitelist Sets the array of classes white-listed; this includes
     * filtering.
     */
    public void setWhitelist(String[] whitelist)
    {
        this.whitelist = Misc.arrayStringUnique(whitelist);
    }
    /**
     * @param type Sets the type of question.
     */
    public void setType(QuestionType type)
    {
        this.type = type;
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The question's text.
     */
    public String getText()
    {
        return text;
    }
    /**
     * @return The question's skeleton code.
     */
    public String getSkeleton()
    {
        return skeleton;
    }
    /**
     * @return Array of classes white-listed; can be empty for no restrictions.
     */
    public String[] getWhitelist()
    {
        return whitelist;
    }
    /**
     * @return Same as {@link #getWhitelist()}, except split by new-line character.
     */
    public String getWhitelistWeb()
    {
        StringBuilder sb = new StringBuilder();
        for(String s : whitelist)
            sb.append(s).append('\n');
        if(sb.length() > 0)
            sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }
    /**
     * @return The type of question.
     */
    public QuestionType getType()
    {
        return type;
    }
}
