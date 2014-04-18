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
 * A model for holding criteria data for a testing inputs question-criteria.
 */
public class JavaTestInputs_Criteria implements Serializable
{
    static final long serialVersionUID = 1L;
    // Fields ******************************************************************
    private String      testCode;
    private String      className;
    private String      method;
    private String[]    inputTypes;
    private String[][]  inputs;
    private boolean     hideSolution;
    // Methods - Constructors **************************************************
    public JavaTestInputs_Criteria()
    {
        this.testCode = this.className = this.method = null;
        inputTypes = new String[0];
        inputs = new String[0][0];
        this.hideSolution = false;
    }
    // Methods - Mutators ******************************************************
    /**
     * @param testCode The code used to produce the same output as the student's
     * code.
     */
    public void setTestCode(String testCode)
    {
        this.testCode = testCode;
    }
    /**
     * @param className The class-name of where the method resides.
     */
    public void setClassName(String className)
    {
        this.className = className;
    }
    /**
     * @param method The name of the method to be tested.
     */
    public void setMethod(String method)
    {
        this.method = method;
    }
    /**
     * @param inputTypes Sets the types of inputs to test; these should be the
     * names of primitive classes.
     * @return True = parsed correctly, false = failed.
     */
    public boolean setInputTypes(String inputTypes)
    {
        // Validate and cleanup the input types
        String[] raw = inputTypes.split(",");
        String[] types = new String[raw.length];
        String s;
        for(int i = 0; i < raw.length; i++)
        {
            s = raw[i].trim().toLowerCase();
            if(s.length() == 0 ||
                        !(
                            s.equals("byte") || s.equals("short") || s.equals("int") || s.equals("long") || s.equals("float") || s.equals("double") ||
                            s.equals("bool") || s.equals("boolean") || s.equals("char") || s.equals("string") || s.equals("str") ||

                            s.equals("byte[]") || s.equals("short[]") || s.equals("int[]") || s.equals("long[]") || s.equals("float[]") || s.equals("double[]") ||
                            s.equals("bool[]") || s.equals("boolean[]") || s.equals("char[]") || s.equals("string[]") || s.equals("str[]")
                        )
                    )
                return false;
            types[i] = s;
        }
        this.inputTypes = types;
        return true;
    }
    /**
     * @param inputs The inputs to be tested; each line should correspond
     * to a set of inputs to test, with each set consisting of the corresponding
     * values for each primitive input-type to be specified. setInputTypes
     * should be invoked first. Values should be separated by semi-colon;
     * a value, which is an array, should separate values using commas.
     * @return True = parsed correctly, false = failed.
     */
    public boolean setInputs(String inputs)
    {
        String[] raw = inputs.replace("\r", "").split("\n");
        int cols = inputTypes.length;
        int rows = raw.length;
        String[][] parsed = new String[rows][cols];
        String[] line;
        // Validate and clean-up each row
        for(int r = 0; r < rows; r++)
        {
            line = raw[r].trim().split(";");
            if(line.length != cols)
                return false;
            for(int c = 0; c < cols; c++)
                line[c] = line[c].trim();
            parsed[r] = line;
        }
        this.inputs = parsed;
        return true;
    }
    /**
     * @param hideSolution Indicates if to hide the solution result.
     */
    public void setHideSolution(boolean hideSolution)
    {
        this.hideSolution = hideSolution;
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The code used to produce the same output as the student's code.
     */
    public String getTestCode()
    {
        return testCode;
    }
    /**
     * @return The name of the class.
     */
    public String getClassName()
    {
        return className;
    }
    /**
     * @return The name of the method.
     */
    public String getMethod()
    {
        return method;
    }
    /**
     * @return The names of the types being tested.
     */
    public String[] getInputTypes()
    {
        return inputTypes;
    }
    /**
     * @return The input-types as a string, for web-display.
     */
    public String getInputTypesWeb()
    {
        StringBuilder sb = new StringBuilder();
        for(String s : inputTypes)
            sb.append(s).append(',');
        if(sb.length() > 0)
            sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }
    /**
     * @return The inputs being tested; each row is a set of inputs, with each
     * column a value.
     */
    public String[][] getInputs()
    {
        return inputs;
    }
    /**
     * @return The inputs being tested, as a string for web-display.
     */
    public String getInputsWeb()
    {
        StringBuilder sb = new StringBuilder();
        String[] t;
        for(int r = 0; r < inputs.length; r++)
        {
            t = inputs[r];
            for(int c = 0; c < t.length; c++)
                sb.append(t[c]).append(';');
            sb.deleteCharAt(sb.length()-1).append('\n');
        }
        if(sb.length() > 0)
            sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }
    /**
     * @return Indicates if to hide the solution.
     */
    public boolean getHideSolution()
    {
        return hideSolution;
    }
}
