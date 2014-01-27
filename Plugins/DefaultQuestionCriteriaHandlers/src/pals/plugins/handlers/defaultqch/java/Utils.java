package pals.plugins.handlers.defaultqch.java;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import org.apache.commons.io.FileUtils;
import pals.base.NodeCore;
import pals.base.Storage;
import pals.base.assessment.InstanceAssignmentQuestion;
import pals.base.web.WebRequestData;
import pals.plugins.handlers.defaultqch.data.CodeJava_Instance;

/**
 * A utilities class for handling Java.
 */
public class Utils
{
    // Methods - Static ********************************************************
    /**
     * @param code The code to be parsed.
     * @return The parsed full-name of the class or null.
     */
    public static String parseFullClassName(String code)
    {
        // Locate the package name and class
        String strPackage, strClass;
        // -- We'll take the first ones we find and use those to form the full-name
        // -- -- We'll also make the package optional
        Pattern pattPackage = Pattern.compile("package (([a-zA-Z]{1})([a-zA-Z0-9.\\_]+)?);");
        Pattern pattClass = Pattern.compile("class ([a-zA-Z0-9]+)");
        Matcher t;
        // -- Package
        t = pattPackage.matcher(code);
        strPackage = t.find() ? t.group(1) : null;
        // -- Class
        t = pattClass.matcher(code);
        strClass = t.find() ? t.group(1) : null;
        
        if(strClass != null)
        {
            if(strPackage != null)
            {
                if(!strPackage.endsWith("."))
                    return strPackage+"."+strClass;
            }
            else
                return strClass;
        }
        return null;
    }
    /**
     * Compiles the code for a question.
     * 
     * @param core  The current instance of the core.
     * @param pathTemp The temporary path to store the output from the
     * compilation; this directory will be recreated.
     * @param code A map of the code (name,source-code).
     * @return The result from the compilation.
     */
    public static CompilerResult compile(NodeCore core, String pathTemp, Map<String,String> code)
    {
        JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
        // Fetch compiler
        if(jc == null)
            return new CompilerResult(CompilerResult.CompileStatus.Failed_CompilerNotFound, null);
        // Recreate the temporary directory
        {
            File f = new File(pathTemp);
            // Delete the directory
            if(f.exists())
            {
                try
                {
                    FileUtils.deleteDirectory(f);
                }
                catch(IOException ex)
                {
                    return new CompilerResult(CompilerResult.CompileStatus.Failed_TempDirectory, null);
                }
            }
            // (Re)create the directory
            if(!f.mkdir())
                return new CompilerResult(CompilerResult.CompileStatus.Failed_TempDirectory, null);
        }
        // Setup virtual file system for compiler
        CompilerFileManager cfm = new CompilerFileManager(jc.getStandardFileManager(null, null, null), pathTemp, false);
        // Add the code to the virtual file-system
        for(Map.Entry<String,String> file : code.entrySet())
            cfm.getClassLoader().add(file.getKey(), file.getValue());
        // Attempt to compile the code
        DiagnosticCollector<JavaFileObject> diag = new DiagnosticCollector<>();
        boolean compiled = jc.getTask(null, cfm, diag, Arrays.asList(new String[]{"-d", pathTemp}), null, cfm.getClassLoader().getCompilerObjects()).call();
        if(!compiled)
            return new CompilerResult(CompilerResult.CompileStatus.Failed, diag);
        else
            return new CompilerResult(CompilerResult.CompileStatus.Success, null);
    }
    /**
     * Adds the required JS and CSS files for CodeMirror text editor, for Java.
     * 
     * @param data The data for the current web-request.
     */
    public static void pageHookCodeMirror_Java(WebRequestData data)
    {
        if(!data.containsTemplateData("codemirror_clike"))
        {
            data.appendHeaderJS("/content/codemirror/lib/codemirror.js");
            data.appendHeaderJS("/content/codemirror/addon/edit/matchbrackets.js");
            data.appendHeaderJS("/content/codemirror/mode/clike/clike.js");
            data.appendHeaderCSS("/content/codemirror/lib/codemirror.css");
        }
    }
}
