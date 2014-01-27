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
        Failed_CompilerNotFound(4, "Compiler not found; please try again or contact an administrator."),
        Failed_TempDirectory(8, "Could not establish temporary shared folder for instance of question; please try again or contact an administrator.");
        
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
            buffer[i] = new CodeError(d.getMessage(Locale.getDefault()), (int)d.getLineNumber(), (int)d.getColumnNumber());
        }
        return buffer;
    }
}
