package pals.plugins.handlers.defaultqch.data;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Saves the response from a user for a Code-Java question instance.
 */
public class CodeJava_Instance implements Serializable
{
    static final long serialVersionUID = 1L;
    // Fields ******************************************************************
    private final HashMap<String,String> code;
    // Methods - Constructors **************************************************
    public CodeJava_Instance()
    {
        this.code = new HashMap<>();
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
}
