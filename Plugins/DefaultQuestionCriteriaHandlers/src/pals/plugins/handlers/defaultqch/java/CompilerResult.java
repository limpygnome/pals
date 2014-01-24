package pals.plugins.handlers.defaultqch.java;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;

/**
 * The result from attempting to compile code.
 */
public class CompilerResult
{
    // Enums *******************************************************************
    public enum CompileStatus
    {
        Success,
        Failed,
        Failed_CompilerNotFound,
        Failed_TempDirectory
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
}
