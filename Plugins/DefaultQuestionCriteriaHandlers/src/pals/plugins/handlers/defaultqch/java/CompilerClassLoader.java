package pals.plugins.handlers.defaultqch.java;

import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * A class-loader used for accessing class-data (such as source-code and byte
 * data) and creating instances of classes from byte-data.
 */
public class CompilerClassLoader extends SecureClassLoader
{
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
    protected Class<?> findClass(String classPath) throws ClassNotFoundException
    {
        CompilerObject obj = objs.get(classPath);
        if(obj == null)
            throw new ClassNotFoundException(classPath);
        byte[] data = obj.getBytes();
        return super.defineClass(obj.getClassNameFull(), data, 0, data.length);
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
        return buffer;
    }
}
