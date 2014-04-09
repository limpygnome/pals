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
    Authors:    Marcus Craske           <limpygnome@gmail.com>
    ----------------------------------------------------------------------------
*/
package pals.base;

/**
 * A class for storing version information.
 * 
 * @version 1.0
 */
public class Version
{
    // Fields ******************************************************************
    private int major, minor, build;
    // Methods - Constructors **************************************************
    /**
     * Creates a new instance of this class.
     * 
     * @param major The version major.
     * @param minor The version minor.
     * @param build The version build.
     * @since 1.0
     */
    public Version(int major, int minor, int build)
    {
        this.major = major;
        this.minor = minor;
        this.build = build;
    }
    // Methods - Mutators ******************************************************
    /**
     * Sets the version major.
     * 
     * @param major The new major version; must be greater than zero.
     * @since 1.0
     */
    public void setMajor(int major)
    {
        if(major <= 0)
            throw new IllegalArgumentException("Invalid major version!");
        this.major = major;
    }
    /**
     * Sets the version minor.
     * 
     * @param minor The new minor version; must be greater than zero.
     * @since 1.0
     */
    public void setMinor(int minor)
    {
        if(minor <= 0)
            throw new IllegalArgumentException("Invalid minor version!");
        this.minor = minor;
    }
    /**
     * Sets the version build.
     * 
     * @param build The new build version; must be greater than zero.
     * @since 1.0
     */
    public void setBuild(int build)
    {
        if(build <= 0)
            throw new IllegalArgumentException("Invalid build version!");
        this.build = build;
    }
    // Methods - Accessors *****************************************************
    /**
     * Retrieves the version major.
     * 
     * @return The major number of the version.
     * @since 1.0
     */
    public int getMajor()
    {
        return major;
    }
    /**
     * Retrieves the version minor.
     * 
     * @return The minor number of the version.
     * @since 1.0
     */
    public int getMinor()
    {
        return minor;
    }
    /**
     * Retrieves the version build.
     * 
     * @return The build number of the version.
     * @since 1.0
     */
    public int getBuild()
    {
        return build;
    }

    // Overrides ***************************************************************
    /**
     * @see Object#equals(java.lang.Object)
     * 
     * @since 1.0
     */
    @Override
    public boolean equals(Object obj)
    {
        if(obj instanceof Version)
        {
            Version t = (Version)obj;
            return t.major == major && t.minor == minor && t.build == build;
        }
        return false;
    }
    /**
     * Outputs the version information.
     * 
     * @return Format: major.minor.build
     * @since 1.0
     */
    @Override
    public String toString()
    {
        return major+"."+minor+"."+build;
    }
}
