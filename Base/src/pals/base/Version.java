package pals.base;

/**
 * A class for storing version information.
 */
public class Version
{
    // Fields ******************************************************************
    private int major, minor, build;
    // Methods - Constructors **************************************************
    public Version(int major, int minor, int build)
    {
        this.major = major;
        this.minor = minor;
        this.build = build;
    }
    // Methods - Mutators ******************************************************
    /**
     * @param major The new major version; must be greater than zero.
     */
    public void setMajor(int major)
    {
        if(major <= 0)
            throw new IllegalArgumentException("Invalid major version!");
        this.major = major;
    }
    /**
     * @param minor The new minor version; must be greater than zero.
     */
    public void setMinor(int minor)
    {
        if(minor <= 0)
            throw new IllegalArgumentException("Invalid minor version!");
        this.minor = minor;
    }
    /**
     * @param build The new build version; must be greater than zero.
     */
    public void setBuild(int build)
    {
        if(build <= 0)
            throw new IllegalArgumentException("Invalid build version!");
        this.build = build;
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The major number of the version.
     */
    public int getMajor()
    {
        return major;
    }
    /**
     * @return The minor number of the version.
     */
    public int getMinor()
    {
        return minor;
    }
    /**
     * @return The build number of the version.
     */
    public int getBuild()
    {
        return build;
    }

    // Overrides ***************************************************************
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
    @Override
    public String toString()
    {
        return major+"."+minor+"."+build;
    }
}
