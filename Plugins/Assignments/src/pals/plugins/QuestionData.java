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
package pals.plugins;

import pals.base.assessment.AssignmentQuestion;
import pals.base.assessment.InstanceAssignmentQuestion;

/**
 * The model passed to the main template for rendering questions.
 */
public class QuestionData
{
    // Fields ******************************************************************
    private int                         number;
    private AssignmentQuestion          question;
    private InstanceAssignmentQuestion  instanceQuestion;
    private String              html;
    // Methods - Constructors **************************************************
    public QuestionData()
    {
        this(-1, null, null, null);
    }
    public QuestionData(int number, AssignmentQuestion question, InstanceAssignmentQuestion instanceQuestion, String html)
    {
        this.number = number;
        this.question = question;
        this.instanceQuestion = instanceQuestion;
        this.html = html;
    }
    // Methods - Mutators ******************************************************
    /**
     * @param number The question's number, as displayed on the page.
     */
    public void setNumber(int number)
    {
        this.number = number;
    }
    /**
     * @param question Sets the question being displayed.
     */
    public void setQuestion(AssignmentQuestion question)
    {
        this.question = question;
    }
    /**
     * @param instanceQuestion Sets the instance of the question; can be null.
     */
    public void setInstanceQuestion(InstanceAssignmentQuestion instanceQuestion)
    {
        this.instanceQuestion = instanceQuestion;
    }
    /**
     * @param html Sets the HTML of the question being displayed.
     */
    public void setHTML(String html)
    {
        this.html = html;
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The question's (display) number.
     */
    public int getNumber()
    {
        return number;
    }
    /**
     * @return Retrieves the question being displayed.
     */
    public AssignmentQuestion getAssignmentQuestion()
    {
        return question;
    }
    /**
     * @return Retrieves the instance of the question; can be null.
     */
    public InstanceAssignmentQuestion getInstanceQuestion()
    {
        return instanceQuestion;
    }
    /**
     * @return Retrieves the HTML of the question being displayed.
     */
    public String getHTML()
    {
        return html;
    }
    /**
     * @return Indicates if the question has instance data.
     */
    public boolean hasInstanceQuestion()
    {
        return instanceQuestion != null;
    }
}
