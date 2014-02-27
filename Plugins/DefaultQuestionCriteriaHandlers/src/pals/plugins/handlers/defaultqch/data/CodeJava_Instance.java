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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import org.apache.commons.io.FileUtils;
import pals.base.NodeCore;
import pals.base.Storage;
import pals.base.assessment.InstanceAssignmentQuestion;
import pals.base.database.Connector;
import pals.plugins.handlers.defaultqch.java.CompilerResult;

/**
 * Saves the response from a user for a Code-Java question instance.
 * 
 * The underlying data-structures are kept null, unless needed, to conserve
 * storage for serialization.
 * 
 * The names of files uploaded are also stored, which reduces I/O latency and
 * calls (since files can be within sub-directories which requires recursion).
 */
public class CodeJava_Instance extends CodeJava_Shared implements Serializable
{
    static final long serialVersionUID = 1L;
    // Fields ******************************************************************
    private         boolean                         prepared;
    private final   ArrayList<CodeError>            errors;
    private         CompilerResult.CompileStatus    status;
    // Methods - Constructors **************************************************
    public CodeJava_Instance()
    {
        this.prepared = false;
        this.errors = new ArrayList<>();
        this.status = CompilerResult.CompileStatus.Unknown;
    }
    // Methods *****************************************************************
    /**
     * Prepares the instance for assessment; this should be invoked by all
     * criteria before marking.
     * 
     * @param core The current instance of the core.
     * @param conn Database connector.
     * @param iaq Instance of question, to which this model belongs.
     * @return Indicates if the operation succeeded, else failed.
     */
    public boolean prepare(NodeCore core, Connector conn, InstanceAssignmentQuestion iaq)
    {
        // Check if we've already been prepared
        if(prepared)
            return true;
        // Fetch dirs
        String shared = core.getPathShared();
        File fSrc = new File(Storage.getPath_tempQuestion(shared, iaq.getAssignmentQuestion().getQuestion()));
        File fDest = new File(Storage.getPath_tempIAQ(shared, iaq));
        // Copy physical files
        if(fSrc.exists() && fDest.exists())
        {
            try
            {
                FileUtils.copyDirectory(fSrc, fDest);
            }
            catch(IOException ex)
            {
                return false;
            }
        }
        // Re-persist this model due to being prepared
        prepared = true;
        iaq.setData(this);
        return iaq.persist(conn) == InstanceAssignmentQuestion.PersistStatus.Success;
    }
    // Methods - Mutators ******************************************************
    /**
     * @param error Adds an error.
     */
    public void errorsAdd(CodeError error)
    {
        errors.add(error);
    }
    /**
     * Clears any errors.
     */
    public void errorClear()
    {
        errors.clear();
    }
    /**
     * @param status The status from a compilation attempt; set to unknown if
     * no compilation has taken place.
     */
    public void setCompileStatus(CompilerResult.CompileStatus status)
    {
        this.status = status;
    }
    /**
     * @param prepared Sets if this instance is prepared for assessment.
     */
    public void setPrepared(boolean prepared)
    {
        this.prepared = prepared;
    }
    // Methods - Accessors *****************************************************
    /**
     * @return Errors from a compilation attempt; can be null or empty.
     */
    public CodeError[] getErrors()
    {
        return errors.toArray(new CodeError[errors.size()]);
    }
    /**
     * @return The status from an attempted compilation; unknown if no
     * compilation has taken place.
     */
    public CompilerResult.CompileStatus getStatus()
    {
        return status;
    }
    /**
     * @return Indicates if this instance has been prepared.
     */
    public boolean isPrepared()
    {
        return prepared;
    }
}
