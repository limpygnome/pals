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
package pals.base;

import java.io.File;
import java.util.Random;
import pals.base.assessment.InstanceAssignmentQuestion;
import pals.base.assessment.Question;
import pals.base.assessment.QuestionCriteria;
import pals.base.web.UploadedFile;

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
     * Checks all of the directories required by this helper-class are present
     * and ready for I/O.
     * 
     * @param pathShared The path to the shared directory.
     * @return True if successfully created/exist, false if failed.
     */
    public static boolean checkFoldersCreated(String pathShared)
    {
        // Templates
        if(!checkCreated(getPath_templates(pathShared)))
        {
            System.err.println("Failed to check folder 'templates' is created.");
            return false;
        }
        // Logs
        if(!checkCreated(getPath_logs(pathShared)))
        {
            System.err.println("Failed to check folder 'logs' is created.");
            return false;
        }
        // Temporary dirs
        // -- Web
        if(!checkCreated(getPath_tempWeb(pathShared)))
        {
            System.err.println("Failed to check folder 'web' is created.");
            return false;
        }
        // -- IAQ
        if(!checkCreated(getPath_tempIAQ(pathShared)))
        {
            System.err.println("Failed to check folder 'iaq' is created.");
            return false;
        }
        // -- Q
        if(!checkCreated(getPath_tempQuestion(pathShared)))
        {
            System.err.println("Failed to check folder 'questions' is created.");
            return false;
        }
        // -- QC
        if(!checkCreated(getPath_tempQC(pathShared)))
        {
            System.err.println("Failed to check folder 'qc' is created.");
            return false;
        }
        return true;
    }
    public static boolean checkCreated(String path)
    {
        File f = new File(path);
        return !f.exists() ? f.mkdir() : true;
    }
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
    /**
     * Temporary web directory; items in this directory should not be left for
     * longer than 15 minutes.
     * 
     * @param pathShared The path of the shared directory/storage.
     * @return The path of the temporary web directory.
     */
    public static String getPath_tempWeb(String pathShared)
    {
        return pathShared + "/temp_web";
    }
    /**
     * Gets a temporary path for a directory; this should be deleted by the
     * system invoking this method.
     * 
     * @param pathShared The path of the shared directory/storage.
     * @param ip The remote address of the user.
     * @return The path for a new directory; this must be created by the
     * invoker.
     */
    public static String getPath_tempWebDir(String pathShared, String ip)
    {
        Random rng = new Random(System.currentTimeMillis());
        return pathShared+"/temp_web/"+ip.replace(":", ".")+"_"+rng.nextInt();
    }
    /**
     * Gets a temporary path for uploading a file; this should be deleted by
     * the system invoking this method.
     * 
     * @param pathShared The path of the shared directory/storage.
     * @param ip The remote address of the user.
     * @return The path for a new file.
     */
    public static String getPath_tempWebFile(String pathShared, String ip)
    {
        return getPath_tempWebDir(pathShared, ip)+".part";
    }
    /**
     * Gets the path of an uploaded-file.
     * 
     * @param pathShared The path of the shared directory/storage.
     * @param file The file used to generate the path.
     * @return The path of the specified file.
     */
    public static String getPath_tempWebFile(String pathShared, UploadedFile file)
    {
        return getPath_tempWeb(pathShared)+"/"+file.getTempName();
    }
    /**
     * @param pathShared The path of the shared directory/storage.
     * @return The root path used for storing temporary data for instance assignment questions.
     */
    public static String getPath_tempIAQ(String pathShared)
    {
        return pathShared + "/temp_iaq";
    }
    /**
     * @param pathShared The path of the shared directory/storage.
     * @return The root path used for storing temporary data for instances of questions.
     */
    public static String getPath_tempQuestion(String pathShared)
    {
        return pathShared + "/temp_q";
    }
    /**
     * @param pathShared The path of the shared directory/storage.
     * @return The root path used for storing temporary data for instances of question-criteria.
     */
    public static String getPath_tempQC(String pathShared)
    {
        return pathShared + "/temp_qc";
    }
    // Methods - Path Construction - Specific **********************************
    /**
     * @param pathShared The path of the shared directory/storage.
     * @param iaq  The instance of the assignment question.
     * @return The path of the temporary directory for the instance of an IAQ.
     */
    public static String getPath_tempIAQ(String pathShared, InstanceAssignmentQuestion iaq)
    {
        return pathShared + "/temp_iaq/"+iaq.getAIQID();
    }
    /**
     * @param pathShared The path of the shared directory/storage.
     * @param q The question instance.
     * @return The temporary path for the instance of the question.
     */
    public static String getPath_tempQuestion(String pathShared, Question q)
    {
        return pathShared + "/temp_q/" + q.getQID();
    }
    /**
     * @param pathShared The path of the shared directory/storage.
     * @param qc The question criteria instance.
     * @return The temporary path for the instance of the question-criteria.
     */
    public static String getPath_tempQC(String pathShared, QuestionCriteria qc)
    {
        return pathShared+"/temp_qc/"+qc.getQCID();
    }
}
