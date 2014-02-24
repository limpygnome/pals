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
import pals.base.web.security.Escaping;

/**
 * Stores the results from a Java test-inputs criteria.
 */
public class JavaTestInputs_InstanceCriteria implements Serializable
{
    static final long serialVersionUID = 1L;
    // Fields ******************************************************************
    private String[]    input;
    private String[]    outputCorrect;
    private String[]    outputStudent;
    // Methods - Constructors **************************************************
    public JavaTestInputs_InstanceCriteria(int numberOfTests)
    {
        input = new String[numberOfTests];
        outputCorrect = new String[numberOfTests];
        outputStudent = new String[numberOfTests];
    }
    // Methods - Mutators ******************************************************
    /**
     * @param index The index of the test.
     * @param value The value to set for the input of the test.
     */
    public void setInput(int index, String value)
    {
        input[index] = value;
    }
    /**
     * @param index The index of the test.
     * @param value The output from the student.
     */
    public void setOutputStudent(int index, String value)
    {
        outputStudent[index] = value;
    }
    /**
     * @param index The The index of the test.
     * @param value The output from the correct solution.
     */
    public void setOutputCorrect(int index, String value)
    {
        outputCorrect[index] = value;
    }
    // Methods - Accessors *****************************************************
    /**
     * @param index The index of the test.
     * @return The input used for the test.
     */
    public String getInput(int index)
    {
        return input[index];
    }
    /**
     * @param index The index of the test.
     * @return The output from the student for the test.
     */
    public String getOutputStudent(int index)
    {
        return outputStudent[index];
    }
    /**
     * @param index The index of the test.
     * @return The output from solution for the test.
     */
    public String getOutputCorrect(int index)
    {
        return outputCorrect[index];
    }
    /**
     * @param index The index of the test.
     * @return The output from the student, with new-lines replaced by HTML
     * line-breaks.
     */
    public String getOutputStudentWeb(int index)
    {
        return Escaping.htmlEncode(outputStudent[index]).replace("\n", "<br />");
    }
    /**
     * @param index The index of the test.
     * @return The output from the solution, with new-lines replaced by
     * HTML line-breaks.
     */
    public String getOutputCorrectWeb(int index)
    {
        return Escaping.htmlEncode(outputCorrect[index]).replace("\n", "<br />");
    }
    /**
     * @return The total number of tests.
     */
    public int getTests()
    {
        return input.length;
    }
    /**
     * @param index The index of the test.
     * @return Indicates if the test was correct.
     */
    public boolean isCorrect(int index)
    {
        return outputCorrect[index].equals(outputStudent[index]);
    }
}
