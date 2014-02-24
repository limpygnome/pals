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
 * Used for feedback.
 */
public class JavaTestProgram_InstanceCriteria implements Serializable
{
    static final long serialVersionUID = 1L;
    // Enums *******************************************************************
    public enum Status
    {
        Info(0),
        Input(1),
        Incorrect(2),
        Correct(4);
        private int value;
        private Status(int value)
        {
            this.value = value;
        }
        public int getValue()
        {
            return value;
        }
    }
    // Fields ******************************************************************
    private ArrayList<String>   lines;
    private ArrayList<Status>   status;
    // Methods - Constructors **************************************************
    public JavaTestProgram_InstanceCriteria()
    {
        this.lines = new ArrayList<>();
        this.status = new ArrayList<>();
    }
    // Methods - Mutators ******************************************************
    public void addLine(String line, Status status)
    {
        this.lines.add(line);
        this.status.add(status);
    }
    // Methods - Accessors *****************************************************
    /**
     * @param index The index of the line.
     * @return The line at the index.
     */
    public String getLine(int index)
    {
        return lines.get(index);
    }
    /**
     * @param index The index of the line.
     * @return The status of the line at the index.
     */
    public Status getStatus(int index)
    {
        return index < status.size() ? status.get(index) : Status.Incorrect;
    }
    /**
     * @return The total number of lines.
     */
    public int getLines()
    {
        return lines.size();
    }
    /**
     * @return The mark given to each correct line.
     */
    public double getCorrectMark()
    {
        return (1.0/(double)lines.size())*100.0;
    }
}
