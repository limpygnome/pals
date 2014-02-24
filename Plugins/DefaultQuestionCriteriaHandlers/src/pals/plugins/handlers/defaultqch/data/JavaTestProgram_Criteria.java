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
import pals.base.utils.Misc;

/**
 * Stores the criteria data for Java test-program.
 */
public class JavaTestProgram_Criteria implements Serializable
{
    static final long serialVersionUID = 1L;
    // Fields ******************************************************************
    private String      epClass,
                        epMethod;
    private String[]    epArgs;
    private String[][]  IO;
    private int         errorThreshold;
    private boolean     mergeStdErr;
    private boolean     hideSolution;
    // Methods - Constructors **************************************************
    public JavaTestProgram_Criteria()
    {
        this.epClass = this.epMethod = null;
        this.epArgs = new String[0];
        this.IO = new String[0][];
        this.errorThreshold = 5;
        this.mergeStdErr = false;
        this.hideSolution = false;
    }
    // Methods - Mutators ******************************************************
    /**
     * @param epClass Sets the entry-point class.
     * @return Indicates if the value is correct.
     */
    public boolean setEpClass(String epClass)
    {
        if(epClass == null || epClass.length() == 0)
            return false;
        this.epClass = epClass;
        return true;
    }
    /**
     * @param epMethod Sets the entry-point method.
     */
    public boolean setEpMethod(String epMethod)
    {
        if(epMethod == null || epMethod.length() == 0)
            return false;
        this.epMethod = epMethod;
        return true;
    }
    /**
     * @param epArgs The entry-point arguments; this is split by new-line, with
     * carriage-return filtered out.
     */
    public void setEpArgs(String epArgs)
    {
        this.epArgs = Misc.arrayStringNonEmpty(epArgs.replace("\r","").split("\n"));
    }
    /**
     * @param IO Sets the expected IO.
     * @return Indicates if the IO has been set correctly.
     */
    public boolean setIO(String IO)
    {
        String[] rows = IO.replace("\r", "").split("\n");
        ArrayList<String[]> buffer = new ArrayList<>();
        String t;
        String[] res;
        int ei;
        boolean hasOutput = false;
        for(int i = 0; i < rows.length; i++)
        {
            t = rows[i].trim();
            ei = t.indexOf('=');
            // Can be empty after equals
            if(ei != -1 && ei > 0 && ei < t.length())
            {
                res = new String[2];
                res[0] = t.substring(0, ei);
                if(!res[0].equals("in") && !res[0].equals("out"))
                    return false;
                else if(res[0].equals("out"))
                    hasOutput = true;
                res[1] = ei+1 < t.length() ? t.substring(ei+1) : "";
                buffer.add(res);
            }
        }
        String[][] tbuff = buffer.toArray(new String[buffer.size()][]);
        if(tbuff.length > 0 && hasOutput)
        {
            this.IO = tbuff;
            return true;
        }
        return false;
    }
    /**
     * @param errorThreshold The threshold for consecutive errors before
     * terminating the program.
     */
    public void setErrorThreshold(int errorThreshold)
    {
        this.errorThreshold = errorThreshold;
    }
    /**
     * @param mergeStdErr Sets if to merge standard error with standard output.
     */
    public void setMergeStdErr(boolean mergeStdErr)
    {
        this.mergeStdErr = mergeStdErr;
    }
    /**
     * @param hideSolution Sets if to hide the solution.
     */
    public void setHideSolution(boolean hideSolution)
    {
        this.hideSolution = hideSolution;
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The entry-point class.
     */
    public String getEpClass()
    {
        return epClass;
    }
    /**
     * @return The entry-point method.
     */
    public String getEpMethod()
    {
        return epMethod;
    }
    /**
     * @return The entry-point arguments.
     */
    public String[] getEpArgs()
    {
        return epArgs;
    }
    /**
     * @return The entry-point arguments in web-format (separated by new-line).
     */
    public String getEpArgsWeb()
    {
        StringBuilder sb = new StringBuilder();
        for(String arg : epArgs)
            sb.append(arg).append("\n");
        if(sb.length() > 0)
            sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }
    /**
     * @return Each row is a line, with column 1 as the type and column 2
     * as the input.
     */
    public String[][] getIO()
    {
        return IO;
    }
    /**
     * @return Gets the IO in web-format, separated by new-lines with equals
     * for separating flow and data.
     */
    public String getIOWeb()
    {
        StringBuilder sb = new StringBuilder();
        for(String[] line : IO)
            sb.append(line[0]).append('=').append(line[1]).append('\n');
        if(sb.length() > 0)
            sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }
    /**
     * @return The threshold for consecutive errors, before terminating the
     * program.
     */
    public int getErrorThreshold()
    {
        return errorThreshold;
    }
    /**
     * @return Indicates if to merge standard error with standard output.
     */
    public boolean getMergeStdErr()
    {
        return mergeStdErr;
    }
    /**
     * @return Indicates if to hide the solution.
     */
    public boolean getHideSolution()
    {
        return hideSolution;
    }
}
