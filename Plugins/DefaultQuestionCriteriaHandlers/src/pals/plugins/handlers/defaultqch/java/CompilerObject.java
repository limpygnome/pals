/*
    The MIT License (MIT)

    Copyright (c) 2014 Marcus Craske <limpygnome@gmail.com>

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in
    all copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
    THE SOFTWARE.
    ----------------------------------------------------------------------------
    Version:    1.0
    Authors:    Marcus Craske           <limpygnome@gmail.com>
    ----------------------------------------------------------------------------
*/
package pals.plugins.handlers.defaultqch.java;

import java.io.IOException;
import java.net.URI;
import javax.tools.SimpleJavaFileObject;

/**
 * Represents an object handled by the Java compiler; this is used for storing
 * the source-code for the compiler and possibly the byte-code from the
 * compilation process.
 */
public class CompilerObject extends SimpleJavaFileObject
{
    // Fields ******************************************************************
    private String source;
    private String className;
    // Methods - Constructors **************************************************
    public CompilerObject(String className, String source)
    {
        super(URI.create("string://"+className.replace(".", "/") + Kind.SOURCE.extension), Kind.SOURCE);
        this.source = source;
        this.className = className;
    }
    // Methods - Mutators ******************************************************
    /**
     * @param source The source-code to set for this object.
     */
    public void setSource(String source)
    {
        this.source = source;
    }
    /**
     * @param className The class-name for this object.
     */
    public void setClassName(String className)
    {
        this.className = className;
    }
    // Methods - Accessors *****************************************************
    /**
     * @param ignoreEncodingError Indicates if to ignore encoding errors; this
     * is not used and simply required for overriding.
     * @return The source-code of the object.
     * @throws IOException Never thrown; required for overriding.
     */
    @Override
    public CharSequence getCharContent(boolean ignoreEncodingError) throws IOException
    {
        return source;
    }
    /**
     * @return Fetches the byte-code of the compiled source for this object;
     * can be null if not compiled in-memory.
     */
    public byte[] getBytes()
    {
        return null;
    }
    /**
     * @return The name of the class of this object.
     */
    public String getClassName()
    {
        String className = this.className;
        int dot = className.indexOf(".");
        return dot != -1 && dot < className.length()-1 ? className.substring(dot+1) : className;
    }
    /**
     * @return The full name of the class, which includes the package name.
     */
    public String getClassNameFull()
    {
        return className;
    }
    /**
     * @return The source-code to be compiled.
     */
    public String getSource()
    {
        return source+".java";
    }
}
