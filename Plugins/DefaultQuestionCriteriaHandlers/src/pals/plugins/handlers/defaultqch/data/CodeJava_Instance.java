package pals.plugins.handlers.defaultqch.data;

import java.io.Serializable;
import java.util.HashMap;
import pals.plugins.handlers.defaultqch.java.CompilerResult;

/**
 * Saves the response from a user for a Code-Java question instance.
 */
public class CodeJava_Instance implements Serializable
{
    static final long serialVersionUID = 1L;
    // Fields ******************************************************************
    private final HashMap<String,String>    code;
    private CodeError[]                     errors;
    private CompilerResult.CompileStatus    status;
    // Methods - Constructors **************************************************
    public CodeJava_Instance()
    {
        this.code = new HashMap<>();
        this.errors = null;
        this.status = CompilerResult.CompileStatus.Unknown;
    }
    // Methods - Code Collection ***********************************************
    /**
     * Removes all of the added classes.
     */
    public void codeClear()
    {
        this.code.clear();
    }
    /**
     * @param className The full class-name of the code being added.
     * @param code The code to be added.
     */
    public void codeAdd(String className, String code)
    {
        this.code.put(className, code);
    }
    /**
     * @param className The full name of the class to remove.
     */
    public void codeRemove(String className)
    {
        this.code.remove(className);
    }
    
    public String codeGetFirst()
    {
        return code.isEmpty() ? null : code.entrySet().iterator().next().getValue();
    }
    /**
     * @return The number of classes provided by the user.
     */
    public int codeSize()
    {
        return this.code.size();
    }
    /**
     * @return The underlying data-structure used to hold code provided by
     * users.
     */
    public HashMap<String,String> getCode()
    {
        return this.code;
    }
    // Methods - Mutators ******************************************************
    /**
     * @param errors Errors from a compilation attempt for this question; can be
     * null.
     */
    public void setErrors(CodeError[] errors)
    {
        this.errors = errors;
    }
    /**
     * @param status The status from a compilation attempt; set to unknown if
     * no compilation has taken place.
     */
    public void setCompileStatus(CompilerResult.CompileStatus status)
    {
        this.status = status;
    }
    // Methods - Accessors *****************************************************
    /**
     * @return Errors from a compilation attempt; can be null or empty.
     */
    public CodeError[] getErrors()
    {
        return errors;
    }
    /**
     * @return The status from an attempted compilation; unknown if no
     * compilation has taken place.
     */
    public CompilerResult.CompileStatus getStatus()
    {
        return status;
    }
}
