package pals.plugins.handlers.defaultqch.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
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
public class CodeJava_Instance implements Serializable
{
    static final long serialVersionUID = 1L;
    // Fields ******************************************************************
    private         TreeMap<String,String>          code;
    private         TreeSet<String>                 files;
    private final   ArrayList<CodeError>            errors;
    private         CompilerResult.CompileStatus    status;
    // Methods - Constructors **************************************************
    public CodeJava_Instance()
    {
        this.code = null;
        this.files = null;
        this.errors = new ArrayList<>();
        this.status = CompilerResult.CompileStatus.Unknown;
    }
    // Methods - File Collection ***********************************************
    public void filesClear()
    {
        if(files != null)
            files.clear();
    }
    public void filesAdd(String fileName)
    {
        if(files == null)
            files = new TreeSet<>();
        files.add(fileName);
    }
    public int filesCount()
    {
        return files == null ? 0 : files.size();
    }
    // Methods - Code Collection ***********************************************
    /**
     * Removes all of the added classes.
     */
    public void codeClear()
    {
        if(code != null)
            code.clear();
    }
    /**
     * @param className The full class-name of the code being added.
     * @param code The code to be added.
     */
    public void codeAdd(String className, String code)
    {
        if(this.code == null)
            this.code = new TreeMap<>();
        this.code.put(className, code);
    }
    /**
     * @param className The full class-name of the source-code to fetch.
     * @return The source-code of the specified class-name, or null.
     */
    public String codeGet(String className)
    {
        return code.get(className);
    }
    /**
     * @param className The full name of the class to remove.
     */
    public void codeRemove(String className)
    {
        if(code != null)
            code.remove(className);
    }
    
    public String codeGetFirst()
    {
        return code == null || code.isEmpty() ? null : code.entrySet().iterator().next().getValue();
    }
    /**
     * @return The number of classes provided by the user.
     */
    public int codeSize()
    {
        return code == null ? 0 : code.size();
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
     * users; can be null.
     */
    public TreeMap<String,String> getCodeMap()
    {
        return code;
    }
    /**
     * @return An array of the names of code files submitted,
     * ordered. Can be empty.
     */
    public String[] getCodeNames()
    {
        if(code == null)
            return new String[0];
        Set<String> names = code.keySet();
        return names.toArray(new String[names.size()]);
    }
    /**
     * @return The underlying data-structure used to hold a list of files;
     * can be null.
     */
    public TreeSet<String> getFilesMap()
    {
        return files;
    }
    /**
     * @return An array of the relative paths of files submitted,
     * ordered. Can be empty.
     */
    public String[] getFileNames()
    {
        return files == null ? new String[0] : files.toArray(new String[files.size()]);
    }
}
