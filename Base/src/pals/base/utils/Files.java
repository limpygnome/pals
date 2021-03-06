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
 * 
 * @version 1.0
 */
public class Files
{
    /**
     * Copies a file, whilst taking care of checking and creating the
     * destination directory.
     * 
     * @param source The source file.
     * @param dest The destination of the file.
     * @param overwrite Indicates if to overwrite if a file exists at dest.
     * @throws FileNotFoundException Thrown if the source file is not found.
     * @throws IOException Thrown if an I/O issue arises.
     * @since 1.0
     */
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
        if(fdRoot != null && !fdRoot.exists())
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
     * @since 1.0
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
     * @since 1.0
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
     * @since 1.0
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
     * @since 1.0
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
     * Fetches all the sub-directories, of the path, which are empty.
     * 
     * @param path The path of the directory to test, exclusive of the array
     * returned.
     * @return Array of directories which are empty; can be empty (array).
     * @since 1.0
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
        if(fl.length == 0 && !topDir)
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
    /**
     * Checks if a file is the child of another file.
     * 
     * @param parent The parent file.
     * @param possibleChild The file that may be a child.
     * @param canBeParent Indicates if the child can be the parent.
     * @return True = is the child, false = is not the child.
     * @since 1.0
     */
    public static boolean isChild(File parent, File possibleChild, boolean canBeParent)
    {
        try
        {
            parent = parent.getCanonicalFile();
            File child = possibleChild.getCanonicalFile();
            while(child != null)
            {
                if(child.equals(parent))
                    return true;
                child = child.getParentFile();
            }
        }
        catch(IOException ex)
        {
        }
        return false;
    }
}
