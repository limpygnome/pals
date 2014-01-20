package pals.plugins.handlers.defaultqch.java;

import java.io.Serializable;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * The class-loader used to access compiled classes.
 * 
 * This uses white-listing, which therefore disallows the usage of any class
 * in any source-code not in the white-list; this allows additional security
 * from potentially dangerous classes.
 */
public class CompilerClassLoader extends SecureClassLoader implements Serializable
{
    static final long serialVersionUID = 1L;
    // Fields ******************************************************************
    private final CompilerFileManager               cfm;
    private final HashMap<String, CompilerObject>   objs;
    private final ArrayList<String>                 whiteList;
    // Methods - Constructors **************************************************
    public CompilerClassLoader(CompilerFileManager cfm)
    {
        this.cfm = cfm;
        this.objs = new HashMap<>();
        this.whiteList = new ArrayList<>();
    }

    // Methods *****************************************************************
    @Override
    protected PermissionCollection getPermissions(CodeSource cs)
    {
        System.err.println("INVOKED");
        return super.getPermissions(cs); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    protected Class<?> findClass(String classPath) throws ClassNotFoundException
    {
        CompilerObject obj = objs.get(classPath);
        if(obj == null)
            throw new ClassNotFoundException(classPath);
        byte[] data = obj.getBytes();
        return super.defineClass(obj.getClassNameFull(), data, 0, data.length);
    }
    /**
     * @param classPath The full class-path of the class to be located.
     * @return An instance of the class.
     * @throws ClassNotFoundException Thrown if the class cannot be loaded.
     * The cause exception may be an instance of WhitelistException; this is
     * thrown if the class uses restricted classes.
     */
    @Override
    public Class<?> loadClass(String classPath) throws ClassNotFoundException
    {
        if(!cfm.isClassWhiteListing() || (!classPath.startsWith("pals.") && whiteList.contains(classPath)))
            return super.loadClass(classPath);
        else
            throw new WhitelistException(classPath);
    }
    // Methods - Collections ***************************************************
    /**
     * Adds source-code for a class-path to be compiled.
     * 
     * @param classPath The full class-path.
     * @param code The code.
     */
    public void add(String classPath, String code)
    {
        objs.put(classPath, new CompilerObjectMemory(classPath, code));
        whiteList.add(classPath);
    }
    /**
     * @param classPath The class-path to be removed.
     */
    public void remove(String classPath)
    {
        objs.remove(classPath);
    }
    /**
     * @param classPath The-path of the item to retrieve.
     * @return The object associated with the class-path; null if not found.
     */
    public CompilerObject get(String classPath)
    {
        return objs.get(classPath);
    }
    // Methods - Whitelist Collection ******************************************
    /**
     * @param classPath The class-path to be white-listed.
     */
    public void whiteListAdd(String classPath)
    {
        whiteList.add(classPath);
    }
    /**
     * @param classPath The class-path to remove from the white-list.
     */
    public void whiteListRemove(String classPath)
    {
        whiteList.remove(classPath);
    }
    /**
     * @return Array of class-paths white-listed; can be empty.
     */
    public String[] whiteListGet()
    {
        return whiteList.toArray(new String[whiteList.size()]);
    }
    // Methods - Accessors *****************************************************
    /**
     * @return An array-list of all the compiler objects; this is useful for the
     * JavaCompiler.
     */
    public ArrayList<CompilerObject> getCompilerObjects()
    {
        ArrayList<CompilerObject> buffer = new ArrayList<>();
        for(Map.Entry<String,CompilerObject> kv : objs.entrySet())
            buffer.add(kv.getValue());
        System.out.println("buffer size: " +buffer.size());
        return buffer;
    }
}
