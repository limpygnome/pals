package pals.base.utils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * A class for handling I/O with JAR (Java Archive) files.
 * @author limpygnome
 */
public class JarIO
{
    // Fields ******************************************************************
    private final   String          path;       // The path of the JAR file.
    private         URLClassLoader  loader;     // Used for accessing a JAR file.
    // Methods - Constructors **************************************************
    private JarIO(String path)
    {
        this.path = path;
    }
    // Methods *****************************************************************
    /**
     * Loads a class from the JAR file.
     * @param fullyQualifiedName The full name of the class, including package.
     * @return Type of the class.
     * @throws JarIOException
     */
    public Class fetchClassType(String fullyQualifiedName) throws JarIOException
    {
        try
        {
            return loader.loadClass(fullyQualifiedName);
        }
        catch(ClassNotFoundException ex)
        {
            throw new JarIOException(path, JarIOException.Reason.ClassNotFound, ex);
        }
    }
    /**
     * Reads a binary resource.
     * @param path The relative path in the JAR file, without a starting slash.
     * @return The byte data of the resource.
     * @throws JarIOException Throws ResourceFetchError.
     */
    public byte[] fetchFile(String path) throws JarIOException
    {
        // Ensure path format is correct
        if(path.startsWith("/") && path.length() > 1)
            path = path.substring(0);
        // Open up a stream to the resource and attempt to read blocks at a time
        // -- Read-line may introduce issues with binary data in the event
        // -- a file does not end with a new-line
        InputStream is = loader.getResourceAsStream(path);
        try
        {
            BufferedInputStream bis = new BufferedInputStream(is);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int len;
            while((len = bis.read(chunk)) != -1)
                baos.write(chunk, 0, len);
            return baos.toByteArray();
        }
        catch(IOException ex)
        {
            throw new JarIOException(path, JarIOException.Reason.ResourceFetchError, ex);
        }
    }
    /**
     * Reads a text file resource as UTF-8.
     * @param path The relative path in the JAR file, without a starting slash.
     * @return The string data from the file.
     * @throws JarIOException Throws ResourceFetchError.
     */
    public String fetchFileText(String path) throws JarIOException
    {
        // Ensure path format is correct
        if(path.startsWith("/") && path.length() > 1)
            path = path.substring(0);
        // Open up a stream and read the file as UTF-8
        InputStream is = loader.getResourceAsStream(path);
        try
        {
            return Files.fileRead(is);
        }
        catch(IOException ex)
        {
            throw new JarIOException(path, JarIOException.Reason.ResourceFetchError, ex);
        }
    }
    /**
     * Fetches a list of files in the archive.
     * @param filter Base directory for files to retrieve. Can be empty or null.
     * @param files Indicates if to retrieve files.
     * @param directories Indicates if to retrieve directories.
     * @return An array of paths of files found matching the specified criteria.
     * @throws JarIOException Throws ResourceFetchError and FileReadPermissions.
     */
    public String[] getFiles(String filter, boolean files, boolean directories) throws JarIOException
    {
        // Check if nothing is to be matched; if so, do no work.
        if(!files && !directories)
            return new String[]{};
        try
        {
            boolean useFilter = filter != null && filter.length() > 0;
            // Iterate each file in the archive
            JarInputStream jis = new JarInputStream(new FileInputStream(path));
            JarEntry je;
            // -- Add each match or file to a result to be returned
            ArrayList<String> result = new ArrayList<>();
            while((je = jis.getNextJarEntry()) != null)
            {
                if((je.isDirectory() && directories) || (!je.isDirectory() && files) && (!useFilter || je.getName().startsWith(filter)))
                {
                    result.add(je.getName());
                }
            }
            return result.toArray(new String[result.size()]);
        }
        catch(IOException ex)
        {
            throw new JarIOException(path, JarIOException.Reason.ResourceFetchError, ex);
        }
        catch(SecurityException ex)
        {
            throw new JarIOException(path, JarIOException.Reason.FileReadPermissions, ex);
        }
    }
    // Methods - Accessors *****************************************************
    /**
     * Gets the absolute path of the JAR.
     * @return Path of JAR; may differ to the original path passed.
     */
    public String getPath()
    {
        return path;
    }
    /**
     * The underlying class-loader.
     * @return Class-loader.
     */
    public ClassLoader getRawLoader()
    {
        return loader;
    }
    // Methods - Static ********************************************************
    /**
     * Opens a JAR file.
     * @param path The path of the file.
     * @return An instance of this class for handling the JAR file.
     * @throws JarIOException Throws FileNotFound, FileReadPermissions and
     * FileReadError.
     */
    public static JarIO open(String path) throws JarIOException
    {
        // Open up a file-handle to the path
        URL fileURL;
        String fullPath;
        {
            File file = new File(path);
            // Check the file exists
            if(!file.exists())
                throw new JarIOException(path, JarIOException.Reason.FileNotFound);
            // Check we have read permissions
            else if(!file.canRead())
                throw new JarIOException(path, JarIOException.Reason.FileReadPermissions);
            // Convert the path to a URL
            try
            {
                fileURL = file.toURI().toURL();
                // Fetch the absolute path of the file, in-case the path provided is relative
                fullPath = file.getCanonicalPath();
            }
            catch(SecurityException | MalformedURLException ex)
            {
                throw new JarIOException(path, JarIOException.Reason.FileReadPermissions, ex);
            }
            catch(IOException ex)
            {
                throw new JarIOException(path, JarIOException.Reason.FileReadError, ex);
            }
        }
        // Create class-loader for JAR file
        JarIO jio = new JarIO(fullPath);
        jio.loader = URLClassLoader.newInstance(new URL[]{fileURL});
        return jio;
    }
    /**
     * Disposes any file streams used by this class.
     */
    public void dispose()
    {
        try
        {
            loader.close();
        }
        catch(IOException ex)
        {
        }
        this.loader = null;
        System.gc();
    }
}
