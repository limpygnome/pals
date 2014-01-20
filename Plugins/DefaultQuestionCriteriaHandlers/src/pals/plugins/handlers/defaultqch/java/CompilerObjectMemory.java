package pals.plugins.handlers.defaultqch.java;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A compiler-object which stores compiled classes in-memory.
 */
public class CompilerObjectMemory extends CompilerObject
{
    // Fields ******************************************************************
    private final ByteArrayOutputStream baos;
    // Methods - COnstructors **************************************************
    public CompilerObjectMemory(String classPath, String source)
    {
        super(classPath, source);
        this.baos = new ByteArrayOutputStream();
    }
    // Methods - Accessors *****************************************************
    /**
     * @return A stream for outputting byte-code from the Java compiler.
     * @throws IOException This is never thrown, but required for overriding.
     */
    @Override
    public OutputStream openOutputStream() throws IOException
    {
        return baos;
    }
    /**
     * @return Fetches the byte-code of the compiled source for this object;
     * can be null if not compiled.
     */
    @Override
    public byte[] getBytes()
    {
        return baos.toByteArray();
    }
    /**
     * @return The byte-stream used for writing the compiled byte-code for the
     * source-code of this object.
     */
    public ByteArrayOutputStream getByteStream()
    {
        return baos;
    }
}
