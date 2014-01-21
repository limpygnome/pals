package pals.plugins.handlers.defaultqch.java;

import java.io.IOException;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

/**
 * Used to store the result from a compilation; this avoids a class from being
 * compiled in memory and loaded into the current runtime - dangerous since
 * a newer class with the same name as a core class could overwrite it.
 */
public class CompilerFileManager extends ForwardingJavaFileManager
{
    // Fields ******************************************************************
    private final CompilerClassLoader   loader;
    private String                      tempPath;
    private boolean                     compileInMemory;
    // Methods - Constructors **************************************************
    /**
     * @param sjf An instance of the standard file-manager.
     * @param tempPath A temporary directory for files; this must exist.
     * @param compileInMemory Indicates if to compile classes in memory; if this
     * is false, the compiled classes are outputted to the temporary path.
     */
    public CompilerFileManager(StandardJavaFileManager sjf, String tempPath, boolean compileInMemory)
    {
        super(sjf);
        this.tempPath = tempPath;
        this.loader = new CompilerClassLoader(this);
        this.compileInMemory = compileInMemory;
    }
    // Methods - Accessors *****************************************************
    /**
     * @param lctn The class-loader associated with the location; this is not
     * used, but required for overriding.
     * @return The class-loader; can be null.
     */
    @Override
    public ClassLoader getClassLoader(Location lctn)
    {
        return loader;
    }
    /**
     * @return The class-loader for storing all of the classes for and from
     * compilation.
     */
    public CompilerClassLoader getClassLoader()
    {
        return loader;
    }
    /**
     * @param location The location for output.
     * @param className The class-name being outputted.
     * @param kind The kind of output.
     * @param sibling The sibling of the object.
     * @return The object for outputting the result of the compilation.
     * @throws IOException Not thrown; required for implementation.
     */
    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException
    {
        if(compileInMemory)
        {
            String fullName = sibling.toUri().toString().substring(9).replace("/", ".");
            fullName = fullName.substring(0, fullName.length() - 5);
            return loader.get(fullName);
        }
        else
            return super.getJavaFileForOutput(location, className, kind, sibling);
    }
}
