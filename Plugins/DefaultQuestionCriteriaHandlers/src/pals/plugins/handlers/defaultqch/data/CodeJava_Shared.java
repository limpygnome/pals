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
package pals.plugins.handlers.defaultqch.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import pals.base.Logging;
import pals.base.NodeCore;
import pals.base.Storage;
import pals.base.assessment.InstanceAssignmentQuestion;
import pals.base.assessment.Question;
import pals.base.web.UploadedFile;
import pals.base.web.WebRequestData;
import pals.plugins.handlers.defaultqch.java.Utils;
import static pals.plugins.handlers.defaultqch.questions.CodeJava.FILEUPLOAD_FILES_LIMIT;

/**
 * An abstract shared class for holding files and code.
 */
public class CodeJava_Shared implements Serializable
{
    static final long serialVersionUID = 1L;
    // Enums *******************************************************************
    public enum ProcessFileResult
    {
        /**
         * Indicates an unknown general error occurred.
         */
        Error,
        /**
         * Indicates the temporary web-file cannot be located.
         */
        Temp_Missing,
        /**
         * Failed to create destination directory.
         */
        Failed_Dest_Dir,
        /**
         * Indicates a temporary directory could not be made.
         */
        Failed_Temp_Dir,
        /**
         * Indicates a ZIP upload is an invalid/malformed archive.
         */
        Invalid_Zip,
        /**
         * Indicates maximum number of files exceeded.
         */
        Maximum_Files,
        /**
         * Indicates the operation was successful.
         */
        Success
    }
    // Fields ******************************************************************
    private         TreeMap<String,String>          code;
    private         TreeSet<String>                 files;
    // Methods - Constructors **************************************************
    public CodeJava_Shared()
    {
        this.code = null;
        this.files = null;
    }
    // Methods *****************************************************************
    /**
     * Resets the file/code storage.
     * 
     * @param basePath The base-path of the physical files.
     * @return True = successful, false = failed/error(s) occurred.
     */
    public boolean reset(File basePath)
    {
        filesClear();
        codeClear();
        try
        {
            if(basePath.exists())
                FileUtils.deleteDirectory(basePath);
        }
        catch(IOException ex)
        {
            return false;
        }
        return true;
    }
    // Methods - File Collection ***********************************************
    /**
     * Processes an uploaded file, which can be a zip-archive (and extracted)
     * or a normal file. Class-files are ignored for security.
     * 
     * @param data The current web-request.
     * @param file The uploaded file to be processed.
     * @param dest The destination path for outputting the files; this
     * should be the IAQ or IAC directory.
     * @return The result from processing the file.
     */
    public ProcessFileResult processFile(WebRequestData data, UploadedFile file, File dest)
    {
        // Check we have a valid file to process
        if(file == null || file.getSize() == 0)
            return ProcessFileResult.Error;
        // Get the path of the file
        File src = new File(Storage.getPath_tempWebFile(data.getCore().getPathShared(), file));
        if(!src.exists())
            return ProcessFileResult.Temp_Missing;
        // Check the destination path
        if(!dest.exists() && !dest.mkdir())
            return ProcessFileResult.Failed_Dest_Dir;
        // Check if the file is a zip
        String fileName = file.getName();
        String contentType = file.getContentType();
        if(contentType.equals("application/zip") || contentType.equals("application/x-zip-compressed") || contentType.equals("application/x-zip") ||
                contentType.equals("application/x-compress") || contentType.equals("application/x-compressed") ||
                contentType.equals("multipart/x-zip") || fileName.toLowerCase().endsWith(".zip"))
        {
            // Create temp dir for extraction
            File fOut = new File(Storage.getPath_tempWebDir(data.getCore().getPathShared(), data.getRequestData().getIpAddress()));
            if(!fOut.mkdir())
                return ProcessFileResult.Failed_Temp_Dir;
            try
            {
                // Iterate the zip archive
                ZipFile zip = new ZipFile(src);
                Enumeration<? extends ZipEntry> ents = zip.entries();
                ZipEntry ent;
                InputStream is;
                // -- Setup buffers
                StringBuilder       codeBuffer;
                InputStreamReader   isr;
                char[]              buffer = new char[4096];
                int                 bufferlen;
                String              code;
                int                 files = 0;
                File                f;
                FileOutputStream    fos;
                while(ents.hasMoreElements())
                {
                    ent = ents.nextElement();
                    if(files >= FILEUPLOAD_FILES_LIMIT)
                        return ProcessFileResult.Maximum_Files;
                    // Place java files into model, other files into model dir
                    else if(!ent.isDirectory())
                    {
                        is = zip.getInputStream(ent);
                        // Read source-files into model
                        if(ent.getName().endsWith(".java"))
                        {
                            codeBuffer = new StringBuilder();
                            isr = new InputStreamReader(is);
                            while((bufferlen = isr.read(buffer, 0, 4096)) != -1)
                                codeBuffer.append(buffer, 0, bufferlen);
                            code = codeBuffer.toString();
                            codeAdd(Utils.parseFullClassName(code), code);
                        }
                        // Read files into IAQ/model dir
                        // -- Ignore .class files, most likely a student/user forgetting to delete their build files
                        // -- -- Remove them to avoid conflict with our own compile process.
                        // -- -- May also pose security risk.
                        else if(!ent.getName().endsWith(".class"))
                        {
                            f = new File(dest, ent.getName());
                            // Ensure all dirs have been created, in-case the file is within a new sub-dir
                            f.getParentFile().mkdirs();
                            // Create new file
                            f.createNewFile();
                            // Open up a stream to the file destination, copy source to dest stream, dispose
                            fos = new FileOutputStream(f);
                            IOUtils.copy(is, fos);
                            fos.flush();
                            fos.close();
                            // Add to model
                            filesAdd(ent.getName());
                        }
                        files++;
                    }
                }
                // Delete directory
                fOut.delete();
            }
            catch(ZipException ex)
            {
                return ProcessFileResult.Invalid_Zip;
            }
            catch(IOException ex)
            {
                data.getCore().getLogging().logEx("[CodeJava]", ex, Logging.EntryType.Warning);
                return ProcessFileResult.Error;
            }
        }
        else if(fileName.toLowerCase().endsWith(".java") && fileName.length() > 5)
        {
            try
            {
                // Read file
                String code;
                {
                    StringBuilder buffer = new StringBuilder();
                    BufferedReader br = new BufferedReader(new FileReader(src));
                    char[] chunk = new char[4096];
                    int len;
                    while((len = br.read(chunk)) != -1)
                        buffer.append(chunk, 0, len);
                    code = buffer.toString();
                }
                // Add code to collection
                codeAdd(Utils.parseFullClassName(code), code);
            }
            catch(FileNotFoundException ex)
            {
                return ProcessFileResult.Temp_Missing;
            }
            catch(IOException ex)
            {
                return ProcessFileResult.Error;
            }
        }
        else
        {
            // Copy the file
            try
            {
                FileUtils.copyFile(src, new File(dest, file.getName()));
                filesAdd(file.getName());
            }
            catch(IOException ex)
            {
                return ProcessFileResult.Error;
            }
        }
        return ProcessFileResult.Success;
    }
    /**
     * Removes all the files in the collection.
     */
    public void filesClear()
    {
        if(files != null)
            files.clear();
    }
    /**
     * @param fileName Adds a file to the collection.
     */
    public void filesAdd(String fileName)
    {
        if(files == null)
            files = new TreeSet<>();
        files.add(fileName);
    }
    // Methods - Code Collection ***********************************************
    /**
     * Removes all of the added classes.
     */
    public void codeClear()
    {
        if(code != null)
            code.clear();
    }
    /**
     * @param className The full class-name of the code being added.
     * @param code The code to be added.
     */
    public void codeAdd(String className, String code)
    {
        if(this.code == null)
            this.code = new TreeMap<>();
        this.code.put(className, code);
    }
    /**
     * @param className The full class-name of the source-code to fetch.
     * @return The source-code of the specified class-name, or null.
     */
    public String codeGet(String className)
    {
        return code.get(className);
    }
    /**
     * @param className The full name of the class to remove.
     */
    public void codeRemove(String className)
    {
        if(code != null)
            code.remove(className);
    }
    /**
     * @return The first code file/source, or null.
     */
    public String codeGetFirst()
    {
        return code == null || code.isEmpty() ? null : code.entrySet().iterator().next().getValue();
    }
    /**
     * @return The number of classes provided by the user.
     */
    public int codeSize()
    {
        return code == null ? 0 : code.size();
    }
    // Methods - Accessors - Files *********************************************
    /**
     * @return The number of files held in the collection.
     */
    public int filesCount()
    {
        return files == null ? 0 : files.size();
    }
    /**
     * @return The underlying data-structure used to hold a list of files;
     * can be null.
     */
    public TreeSet<String> getFilesMap()
    {
        return files;
    }
    /**
     * @return An array of the relative paths of files submitted,
     * ordered. Can be empty.
     */
    public String[] getFileNames()
    {
        return files == null ? new String[0] : files.toArray(new String[files.size()]);
    }
    // Methods - Accessors - Code **********************************************
    /**
     * @return A double dimension array, where each row contains the following:
     * 0 - the index of the code (int).
     * 1 - the name of the code.
     * 2 - the code.
     */
    public Object[][] getCode()
    {
        if(code == null)
            return new Object[0][];
        Object[][] buffer = new Object[code.size()][];
        int offset = 0;
        for(Map.Entry<String,String> i : code.entrySet())
            buffer[offset++] = new Object[]{offset, i.getKey(), i.getValue()};
        return buffer;
    }
    /**
     * @return The underlying data-structure used to hold code provided by
     * users; can be null.
     */
    public TreeMap<String,String> getCodeMap()
    {
        return code;
    }
    /**
     * @return An array of the names of code files submitted,
     * ordered. Can be empty.
     */
    public String[] getCodeNames()
    {
        if(code == null)
            return new String[0];
        Set<String> names = code.keySet();
        return names.toArray(new String[names.size()]);
    }
    /**
     * Prepares.
     * 
     * @param core Current instance of core.
     * @param iaq The instance of an assignment question.
     * @return Indicates if the operation succeeded without error.
     */
    public static boolean copyQuestionFiles(NodeCore core, Question q, InstanceAssignmentQuestion iaq)
    {
        // Fetch dirs
        String shared = core.getPathShared();
        File fSrc = new File(Storage.getPath_tempQuestion(shared, q));
        File fDest = new File(Storage.getPath_tempIAQ(shared, iaq));
        // Copy physical files
        if(fSrc.exists() && fDest.exists())
        {
            try
            {
                FileUtils.copyDirectory(fSrc, fDest);
            }
            catch(IOException ex)
            {
                return false;
            }
        }
        return true;
    }
    /**
     * Copies code from a src to dest class inheriting this class.
     * 
     * @param src The source class with code.
     * @param dest The destination class to which src code will be copied.
     * @return The new collection of code, for compilation.
     */
    public static TreeMap<String,String> copyCode(CodeJava_Shared src, CodeJava_Shared dest)
    {
        TreeMap<String,String> map = new TreeMap<>();
        // Copy dest code
        if(dest != null && dest.code != null)
        {
            for(Map.Entry<String,String> kv : dest.code.entrySet())
                map.put(kv.getKey(), kv.getValue());
        }
        // Copy src code
        if(src != null && src.code != null)
        {
            for(Map.Entry<String,String> kv : src.code.entrySet())
                map.put(kv.getKey(), kv.getValue());
        }
        return map;
    }
}
