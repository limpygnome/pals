package pals.plugins.handlers.defaultqch.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import pals.plugins.handlers.defaultqch.java.CompilerResult;

/**
 * Saves the response from a user for a Code-Java question instance.
 */
public class CodeJava_Instance implements Serializable
{
    static final long serialVersionUID = 1L;
    // Fields ******************************************************************
    private final TreeMap<String,String>    code;
    private final ArrayList<CodeError>      errors;
    private CompilerResult.CompileStatus    status;
    // Methods - Constructors **************************************************
    public CodeJava_Instance()
    {
        this.code = new TreeMap<>();
        this.errors = new ArrayList<>();
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
    // Methods - Mutators ******************************************************
    /**
     * @param error Adds an error.
     */
    public void errorsAdd(CodeError error)
    {
        this.errors.add(error);
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
     * @return A double dimension array, where each row contains the following:
     * 0 - the index of the code (int).
     * 1 - the name of the code.
     * 2 - the code.
     */
    public Object[][] getCode()
    {
        Object[][] buffer = new Object[code.size()][];
        int offset = 0;
        for(Map.Entry<String,String> i : code.entrySet())
            buffer[offset++] = new Object[]{offset, i.getKey(), i.getValue()};
        return buffer;
    }
    /**
     * @return The underlying data-structure used to hold code provided by
     * users.
     */
    public TreeMap<String,String> getCodeMap()
    {
        return this.code;
    }
}
