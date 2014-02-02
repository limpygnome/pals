package pals.plugins.handlers.defaultqch.java;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import org.apache.commons.io.FileUtils;
import pals.base.NodeCore;
import pals.base.utils.PalsProcess;
import pals.base.web.WebRequestData;

/**
 * A utilities class for handling Java.
 */
public class Utils
{
    // Fields - Static *********************************************************
    private final static Pattern patternRandom;
    static
    {
        patternRandom = Pattern.compile("^rand\\(([0-9]+):([0-9]+)\\)$");
    }
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
     * @param fullName The full name of the class to parse.
     * @return The class of the full-name; supports primitives simply by
     * their name, as well as String by its short name.
     */
    public static Class parseClass(String fullName)
    {
        if(fullName == null)
            return null;
        switch(fullName)
        {
            case "byte":
                return byte.class;
            case "short":
                return short.class;
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "float":
                return float.class;
            case "double":
                return double.class;
            case "boolean":
            case "bool":
                return boolean.class;
            case "char":
                return char.class;
            case "String":
            case "string":
                return String.class;
            default:
                try
                {
                    return Class.forName(fullName);
                }
                catch(ClassNotFoundException ex)
                {
                    return null;
                }
        }
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
    /**
     * Builds the arguments for the Java Sandbox.
     * 
     * @param javaSandboxPath The path to the Java sandbox program.
     * @param directory The directory of where the compiled class files reside.
     * @param className The class-name of the method to invoke.
     * @param method The method to invoke.
     * @param whiteListedClasses An array of white-listed classes.
     * @param outputValue
     * @param timeout
     * @param inputTypes
     * @param inputs
     * @return 
     */
    public static String[] buildJavaSandboxArgs(String javaSandboxPath, String directory, String className, String method, String[] whiteListedClasses, boolean outputValue, int timeout, String[] inputTypes, String[] inputs)
    {
        if(inputTypes.length != inputs.length)
            throw new IllegalArgumentException();
        
        final int BASE_ARGS = 8;
        String[] buffer = new String[BASE_ARGS+inputs.length];
        // Setup base args
        buffer[0] = "-jar";
        buffer[1] = PalsProcess.formatPath(javaSandboxPath);
        buffer[2] = PalsProcess.formatPath(directory);
        buffer[3] = className;
        buffer[4] = method;
        buffer[5] = buildJavaSandboxArgs_whiteList(whiteListedClasses);
        buffer[6] = outputValue ? "1" : "0";
        buffer[7] = Integer.toString(timeout);
        // Setup input args
        for(int i = 0; i < inputs.length; i++)
            buffer[BASE_ARGS+i] = inputTypes[i]+"="+inputs[i];
        return buffer;
    }
    public static void formatInputs(NodeCore core, String[] inputTypes, String[] inputRow)
    {
        for(int i = 0; i < inputRow.length; i++)
        {
            if(inputRow[i].startsWith("rand"))
                inputRow[i] = formatInputs_inputRandom(core, inputTypes[i], inputRow[i]);
        }
    }
    private static String formatInputs_inputRandom(NodeCore core, String type, String input)
    {
        Matcher m = patternRandom.matcher(input);
        // Check we have a match...
        if(!m.matches())
            return input;
        // Parse min and max range
        String  rawMin = m.group(1),
                rawMax = m.group(2);
        try
        {
            int min = Integer.parseInt(rawMin);
            int max = Integer.parseInt(rawMax);
            if(min >= max)
                return input;
            // Fetch RNG and produce random value
            int rv = core.getRNG().nextInt(max-min+1)-min;
            // Parse input
            switch(type)
            {
                case "byte":
                    return Byte.toString((byte)rv);
                case "short":
                    return Short.toString((short)rv);
                case "integer":
                case "int":
                    return Integer.toString(rv);
                case "long":
                    return Long.toString((long)rv);
                case "float":
                    return Float.toString((float)rv);
                case "double":
                    return Double.toString((double)rv);
                case "bool":
                case "boolean":
                    return Boolean.toString(rv % 2 == 1);
                case "char":
                    return Character.toString((char)rv);
            }
        }
        catch(NumberFormatException ex) {}
        return input;
    }
    private static String buildJavaSandboxArgs_whiteList(String[] whiteListedClasses)
    {
        if(whiteListedClasses.length == 0)
            return "0";
        StringBuilder sb = new StringBuilder();
        for(String s : whiteListedClasses)
            sb.append(s).append(',');
        sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }
}
