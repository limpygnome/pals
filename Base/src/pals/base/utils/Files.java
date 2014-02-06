package pals.base.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * A class of general utility functions for interfacing with the file-system.
 */
public class Files
{
    public static void fileCopy(String source, String dest, boolean overwrite) throws FileNotFoundException, IOException
    {
        File fs = new File(source);
        File fd = new File(dest);
        // Check the source exists
        if(!fs.exists())
            throw new FileNotFoundException("Source file '" + source + "' does not exist.");
        // Check if to overwrite
        if(fd.exists() && !overwrite)
            throw new IOException("Destination '" + dest + "' already exists!");
        // Check the root dir exists
        File fdRoot = fd.getParentFile();
        if(!fdRoot.exists())
            fdRoot.mkdir();
        // Open up the source file and copy the contents to the destination
        try
        {
            fd.createNewFile();
        }
        catch(IOException ex)
        {
            throw new IOException("Failed to create destination file at '" + dest + "'!", ex);
        }
        FileInputStream fis = new FileInputStream(fs);
        FileOutputStream fos = new FileOutputStream(fd);
        byte[] chunk = new byte[4096];
        int len;
        while((len = fis.read(chunk)) != -1)
        {
            fos.write(chunk, 0, len);
        }
        fis.close();
        fos.close();
    }
    /**
     * Reads a file from the specified path.
     * 
     * @param path The path of the file.
     * @return The text of the file, read as UTF-8.
     * @throws IOException Thrown if an IO error occurs.
     * @throws FileNotFoundException Thrown if the specified file is not found.
     */
    public static String fileRead(String path) throws IOException, FileNotFoundException
    {
        return fileRead(new FileInputStream(new File(path)));
    }
    /**
     * Reads a file/data from an input-stream.
     * @param is The input-stream of data.
     * @return The text of the file, read as UTF-8.
     * @throws IOException Thrown if an IO error occurs.
     */
    public static String fileRead(InputStream is) throws IOException
    {
        if(is == null)
            throw new IOException("Invalid input-stream!");
        // Open up a stream and read the data as UTF-8
        InputStreamReader isr = new InputStreamReader(is, "UTF-8");
        BufferedReader br = new BufferedReader(isr);
        StringBuilder sb = new StringBuilder();
        char[] chunk = new char[4096];
        int len;
        while((len = br.read(chunk)) != -1)
            sb.append(chunk, 0, len);
        br.close();
        return sb.toString();
    }
    /**
     * Writes data to file.
     * 
     * Note: this does not append data to the file.
     * @param path The path of the file.
     * @param data The data to write to the file.
     * @param overwrite Indicates if to overwrite the file.
     * @throws IOException Thrown if an error occurs writing the data to file.
     */
    public static void fileWrite(String path, String data, boolean overwrite) throws IOException
    {
        File f = new File(path);
        // Check the file exists, else create it
        if(!f.exists())
            f.createNewFile();
        // Open a stream to the file and write the data
        FileWriter fw = new FileWriter(f);
        
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(data);
        bw.flush();
        
        fw.flush();
        fw.close();
    }
    /**
     * Fetches an array of all the files/directories available at a path.
     * 
     * @param path The path of the folder.
     * @param includeDirs Indicates if to include directories.
     * @param includeFiles Indicates if to include files.
     * @param extFilter Filters files by a specified extension; can be null.
     * @param subDirs Indicates if to recursively iterate sub-directory items.
     * @return Array of file objects to represent the sub-files.
     * @throws FileNotFoundException Thrown if the directory cannot be found.
     */
    public static File[] getAllFiles(String path, boolean includeDirs, boolean includeFiles, String extFilter, boolean subDirs) throws FileNotFoundException
    {
        // Check we have work to do - else return an empty array
        if(!includeDirs && !includeFiles)
            return new File[0];
        // Grab the top directory and iterate items
        ArrayList<File> files = new ArrayList<>();
        File f = new File(path);
        // Check the dir exists and it's a directory
        if(!f.exists() || !f.isDirectory())
            throw new FileNotFoundException("Directory '" + path + "' does not exist or it's not a direcory!");
        // Recursively iterate all dirs to build a tree of files
        getFiles(f, files, includeDirs, includeFiles, extFilter, subDirs);
        return files.toArray(new File[files.size()]);
    }
    private static void getFiles(File dir, ArrayList<File> files, boolean includeDirs, boolean includeFiles, String extFilter, boolean subDirs)
    {
        for(File f : dir.listFiles())
        {
            // Decide if to add the file/directory
            if(( (includeDirs && f.isDirectory()) || (includeFiles && f.isFile())) && (extFilter == null || f.getName().endsWith(extFilter)))
                files.add(f);
            // Iterate the items of the directory
            if(subDirs && f.isDirectory())
                getFiles(f, files, includeDirs, includeFiles, extFilter, subDirs);
        }
    }
    /**
     * @param path The path of the directory to test, exclusive of the array
     * returned.
     * @return Array of directories which are empty; can be empty (array).
     */
    public static File[] getDirsEmpty(String path)
    {
        File f = new File(path);
        if(!f.exists())
            return new File[0];
        ArrayList<File> dirs = new ArrayList<>();
        // Recurse sub-dirs of specified path
        getDirsEmptyRecursive(f, dirs, true);
        return dirs.toArray(new File[dirs.size()]);
    }
    private static void getDirsEmptyRecursive(File dir, ArrayList<File> dirs, boolean topDir)
    {
        File[] fl = dir.listFiles();
        // Check if the current directory is empty
        if(fl.length == 0)
            dirs.add(dir);
        else
        {
            // Iterate sub-dirs
            for(File f : dir.listFiles())
            {
                if(f.isDirectory())
                    getDirsEmptyRecursive(f, dirs, false);
            }
        }
    }
}
