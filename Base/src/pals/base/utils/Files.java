package pals.base.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * A class of general utility functions for interfacing with the file-system.
 */
public class Files
{
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
        // Open up a stream and read the data as UTF-8
        InputStreamReader isr = new InputStreamReader(is, "UTF-8");
        BufferedReader br = new BufferedReader(isr);
        StringBuilder sb = new StringBuilder();
        char[] chunk = new char[4096];
        int len;
        while((len = br.read(chunk)) != -1)
            sb.append(chunk, 0, len);
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
}
