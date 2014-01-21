package pals.javasandbox;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;

/**
 * A class-loader which can restrict the available classes to a white-list.
 * 
 * This is an extension of the URLClassLoader.
 */
public class SandboxRestrictedLoader extends URLClassLoader
{
    // Fields ******************************************************************
    private boolean         whiteListEnabled;
    private HashSet<String> whiteList;
    // Methods - Constructors **************************************************
    public SandboxRestrictedLoader(URL[] urls)
    {
        super(urls);
        this.whiteListEnabled = false;
        this.whiteList = new HashSet<>();
    }
    // Methods - Overrides *****************************************************
    @Override
    public Class<?> loadClass(String className) throws ClassNotFoundException
    {
        if(!whiteListEnabled || whiteList.contains(className))
            return super.loadClass(className);
        else
            throw new WhitelistException(className);
    }
    // Methods - Mutators ******************************************************
    /**
     * @param whiteListEnabled Sets if white-listing is enabled.
     */
    public void setWhiteListEnabled(boolean whiteListEnabled)
    {
        this.whiteListEnabled = whiteListEnabled;
    }
    /**
     * @param className The class to be added to the white-list.
     */
    public void whiteListAdd(String className)
    {
        if(className != null && className.length() > 0 && !whiteList.contains(className))
            whiteList.add(className);
    }
    /**
     * @param className The class to be removed from the white-list.
     */
    public void whiteListRemove(String className)
    {
        whiteList.remove(className);
    }
    // Methods - Accessors *****************************************************
    /**
     * @return Indicates if white-listing is enabled.
     */
    public boolean isWhiteListEnabled()
    {
        return whiteListEnabled;
    }
    /**
     * @return Array of all the white-listed classes.
     */
    public String[] getWhiteListClasses()
    {
        return whiteList.toArray(new String[whiteList.size()]);
    }
}
