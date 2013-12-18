package pals.base;

import java.io.File;

/**
 * A static class responsible for delegating the storage of general files.
 */
public class Storage
{
    // Enums *******************************************************************
    public enum StorageAccess
    {
        DoesNotExist,
        CannotRead,
        CannotWrite,
        CannotExecute,
        Ready
    }
    // Methods - Static ********************************************************
    /**
     * Checks the access to a path.
     * 
     * @param path The path of a file/directory.
     * @param isDir True = path is a directory, false = path is a file.
     * @param requiresRead Indicates if to check read permissions.
     * @param requiresWrite Indicates if to check write permissions.
     * @param requiresExecute Indicates if to check execute permissions.
     * @return The access available to the path.
     */
    public static StorageAccess checkAccess(String path, boolean isDir, boolean requiresRead, boolean requiresWrite, boolean requiresExecute)
    {
        // Validate path param
        if(path == null || path.length() == 0)
            return StorageAccess.DoesNotExist;
        // Check the item exists
        File file = new File(path);
        if(!file.exists() || (isDir && !file.isDirectory()) || (!isDir && file.isDirectory()))
            return StorageAccess.DoesNotExist;
        // Check permissions
        if(requiresRead && !file.canRead())
            return StorageAccess.CannotRead;
        if(requiresWrite && !file.canWrite())
            return StorageAccess.CannotWrite;
        if(requiresExecute && !file.canExecute())
            return StorageAccess.CannotExecute;
        
        return StorageAccess.Ready;
    }
    // Methods - Path Construction *********************************************
    /**
     * Temporary web directory; items in this directory should not be left for
     * longer than 15 minutes.
     * 
     * @param pathShared The path of the shared directory/storage.
     * @return The path of the temporary web directory.
     */
    public static String getPath_webTemp(String pathShared)
    {
        return pathShared + "/temp_web";
    }
    /**
     * The templates directory. Any core templates, not belonging to plugins,
     * should be stored in this directory.
     * 
     * @param pathShared The path of the shared directory/storage.
     * @return The path of the templates directory.
     */
    public static String getPath_templates(String pathShared)
    {
        return pathShared + "/templates";
    }
    /**
     * The root-path for all log files.
     * 
     * @param pathShared The path of the shared directory/storage.
     * @return The path of the logs directory.
     */
    public static String getPath_logs(String pathShared)
    {
        return pathShared + "/logs";
    }
}
