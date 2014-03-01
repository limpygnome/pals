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
package pals.plugins.handlers.defaultqch.java;

import java.util.List;
import java.util.Locale;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import pals.plugins.handlers.defaultqch.data.CodeError;

/**
 * The result from attempting to compile code.
 */
public class CompilerResult
{
    // Enums *******************************************************************
    public enum CompileStatus
    {
        Unknown(0, "This question has not been compiled."),
        Success(1, "Compiled."),
        Failed(2, "Failed to compile."),
        Failed_NoCode(4, "No code provided."),
        Failed_CompilerNotFound(8, "Compiler not found; please try again or contact an administrator."),
        Failed_TempDirectory(16, "Could not establish temporary shared folder for instance of question; please try again or contact an administrator.");
        
        private final int     formValue;
        private final String  text;
        private CompileStatus(int formValue, String text)
        {
            this.formValue = formValue;
            this.text = text;
        }
        public int getFormValue()
        {
            return formValue;
        }
        public String getText()
        {
            return text;
        }
    }
    // Fields ******************************************************************
    private CompileStatus                       status;
    private DiagnosticCollector<JavaFileObject> errors;
    // Methods - Constructors **************************************************
    public CompilerResult(CompileStatus status, DiagnosticCollector<JavaFileObject> errors)
    {
        this.status = status;
        this.errors = errors;
    }
    // Methods - Mutators ******************************************************
    /**
     * @param status The status from compilation.
     */
    public void setStatus(CompileStatus status)
    {
        this.status = status;
    }
    /**
     * @param errors Any errors/diagnostic information from the compiler; this
     * can be null.
     */
    public void setErrors(DiagnosticCollector<JavaFileObject> errors)
    {
        this.errors = errors;
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The status of the attempted compilation.
     */
    public CompileStatus getStatus()
    {
        return status;
    }
    /**
     * @return Any diagnostics/errors; can be null.
     */
    public DiagnosticCollector<JavaFileObject> getErrors()
    {
        return errors;
    }
    /**
     * @return Transforms the compiler errors into local errors; this may
     * return an empty array, but never null.
     */
    public CodeError[] getCodeErrors()
    {
        if(errors == null)
            return new CodeError[0];
        List<Diagnostic<? extends JavaFileObject>> arr = errors.getDiagnostics();
        CodeError[] buffer = new CodeError[arr.size()];
        Diagnostic d;
        for(int i = 0; i < arr.size(); i++)
        {
            d = arr.get(i);
            buffer[i] = new CodeError(((CompilerObjectMemory)d.getSource()).getClassNameFull(), d.getMessage(Locale.getDefault()), (int)d.getLineNumber(), (int)d.getColumnNumber());
        }
        return buffer;
    }
}
