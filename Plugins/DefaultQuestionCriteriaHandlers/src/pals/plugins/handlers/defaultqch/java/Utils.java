package pals.plugins.handlers.defaultqch.java;

import java.io.File;
import java.io.FileNotFoundException;
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
import pals.base.utils.Files;
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
     * @param cl A class-loader to also be used to find a class; can be null.
     * @return The class of the full-name; supports primitives simply by
     * their name, as well as String by its short name.
     */
    public static Class parseClass(String fullName, ClassLoader cl)
    {
        if(fullName == null)
            return null;
        switch(fullName)
        {
            case "byte":
                return byte.class;
            case "byte[]":
                return byte[].class;
            case "short":
                return short.class;
            case "short[]":
                return short[].class;
            case "int":
                return int.class;
            case "int[]":
                return int[].class;
            case "long":
                return long.class;
            case "long[]":
                return long[].class;
            case "float":
                return float.class;
            case "float[]":
                return float[].class;
            case "double":
                return double.class;
            case "double[]":
                return double[].class;
            case "boolean":
            case "bool":
                return boolean.class;
            case "boolean[]":
            case "bool[]":
                return boolean[].class;
            case "char":
                return char.class;
            case "char[]":
                return char[].class;
            case "String":
            case "string":
                return String.class;
            case "String[]":
            case "string[]":
                return String[].class;
            default:
                // Try custom class-loader
                try
                {
                    return Class.forName(fullName, true, cl);
                }
                catch(ClassNotFoundException ex)
                {
                }
                // Try the runtime class-loader
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
        // Reset directory
        File f = new File(pathTemp);
        try
        {
            // Ensure dir exists
            if(!f.exists())
            {
                if(!f.mkdir())
                    return new CompilerResult(CompilerResult.CompileStatus.Failed_TempDirectory, null);
            }
            else
            {
                // Purge .class files
                File[] t = Files.getAllFiles(pathTemp, false, true, ".class", true);
                for(File c : t)
                    c.delete();
                // Remove empty directories
                t = Files.getDirsEmpty(pathTemp);
                for(File c : t)
                    c.delete();
            }
        }
        catch(IOException ex)
        {
            return new CompilerResult(CompilerResult.CompileStatus.Failed_TempDirectory, null);
        }
        // Setup virtual file system for compiler
        CompilerFileManager cfm = new CompilerFileManager(jc.getStandardFileManager(null, null, null), pathTemp, false);
        // Add the code to the virtual file-system
        for(Map.Entry<String,String> file : code.entrySet())
            cfm.getClassLoader().add(file.getKey(), file.getValue());
        // Attempt to compile the code
        DiagnosticCollector<JavaFileObject> diag = new DiagnosticCollector<>();
        boolean compiled = jc.getTask(null, cfm, diag, Arrays.asList(new String[]{"-d", f.getAbsolutePath()}), null, cfm.getClassLoader().getCompilerObjects()).call();
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
     * @param core The current instance of the core.
     * @param javaSandboxPath The path to the Java sandbox program.
     * @param directory The directory of where the compiled class files reside.
     * @param className The class-name of the method to invoke.
     * @param method The method to invoke.
     * @param whiteListedClasses An array of white-listed classes.
     * @param outputValue Indicates if to output the value from the method invoked.
     * @param inputsArgs Array of input arguments for the Java sandbox.
     * @return Arguments for launching sandbox program.
     */
    public static String[] buildJavaSandboxArgs(NodeCore core, String javaSandboxPath, String directory, String className, String method, String[] whiteListedClasses, boolean outputValue, String[] inputsArgs)
    {
        final int BASE_ARGS = 8;
        String[] buffer = new String[BASE_ARGS+inputsArgs.length];
        // Setup base args
        buffer[0] = "-jar";
        buffer[1] = PalsProcess.formatPath(javaSandboxPath);
        buffer[2] = PalsProcess.formatPath(directory);
        buffer[3] = className;
        buffer[4] = method;
        buffer[5] = buildJavaSandboxArgs_whiteList(whiteListedClasses);
        buffer[6] = outputValue ? "1" : "0";
        buffer[7] = Integer.toString(core.getSettings().getInt("tools/javasandbox/timeout_ms", 10000));
        // Setup input args
        for(int i = 0; i < inputsArgs.length; i++)
            buffer[BASE_ARGS+i] = inputsArgs[i];
        return buffer;
    }
    /**
     * Formats the specified input-arguments, allowing for random numbers etc.
     * 
     * @param core Current instance of the core.
     * @param inputArgs The input arguments, for the Java sandbox, to be
     * formatted.
     */
    public static void formatInputs(NodeCore core, String[] inputArgs)
    {
        int             ind;        // Index of =
        String          k;          // Key for argument.
        String[]        v;          // Value(s) for an argument.
        String          t;          // Specific value from v.
        StringBuilder   buffer;     // Used to rebuild args.
        for(int i = 0; i < inputArgs.length; i++)
        {
            // Fetch entire argument
            k = inputArgs[i];
            // Fetch values for argument; may be array, so split by ,
            ind = k.indexOf('=');
            v = ind == k.length()-1 ? new String[0] : inputArgs[i].substring(ind+1).split(",");
            // Fetch key
            k = k.substring(0, ind);
            // Iterate and format each value; we will also rebuild the arg at the same time
            buffer = new StringBuilder();
            buffer.append(k).append("=");
            for(int j = 0; j < v.length; j++)
            {
                t = v[j];
                // Format current value for random value
                if(t.startsWith("rand("))
                    t = formatInputs_inputRandom(core, k, t);
                buffer.append(t).append(",");
            }
            // Rebuild argument
            if(v.length > 0)
                buffer.deleteCharAt(buffer.length()-1);
            inputArgs[i] = buffer.toString();
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
                case "byte:arr":
                    return Byte.toString((byte)rv);
                case "short":
                case "short:arr":
                    return Short.toString((short)rv);
                case "integer":
                case "integer:arr":
                case "int":
                case "int:arr":
                    return Integer.toString(rv);
                case "long":
                case "long:arr":
                    return Long.toString((long)rv);
                case "float":
                case "float:arr":
                    return Float.toString((float)rv);
                case "double":
                case "double:arr":
                    return Double.toString((double)rv);
                case "bool":
                case "bool:arr":
                case "boolean":
                case "boolean:arr":
                    return Boolean.toString(rv % 2 == 1);
                case "char":
                case "char:arr":
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
