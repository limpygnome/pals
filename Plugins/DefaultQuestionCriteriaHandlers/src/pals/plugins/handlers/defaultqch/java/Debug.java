package pals.plugins.handlers.defaultqch.java;

import java.io.File;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Locale;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;


public class Debug
{
    public static void main(String[] args) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException, ClassNotFoundException, MalformedURLException
    {
        // Fetch the compiler
        JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
        if(jc == null)
        {
            System.err.println("[DEBUG TEST] Cannot access JVM.");
            return;
        }
        // Create file-system for handling I/O of source-code and byte-data
        CompilerFileManager cfm = new CompilerFileManager(jc.getStandardFileManager(null, null, null), "debug_output", false);
        // Add classes
        StringBuilder code;
        // -- File 0
        code = new StringBuilder();
        code.append("package test;").append("\n");
        code.append("import java.io.IOException;").append("\n");
        code.append("public class Main").append("\n");
        code.append("{").append("\n");
        code.append("   public static void main(int value, double value2) throws IOException").append("\n");
        code.append("   {").append("\n");
        code.append("       System.out.println(\"test values: \"+value+\",\"+value2);").append("\n");
        code.append("       System.out.println(\"test main!\");").append("\n");
        code.append("       Debug d = new Debug();").append("\n");
        code.append("       System.out.println(\"Invoke #1: \"+d.getTest());").append("\n");
        code.append("       System.out.println(\"Invoke #2: \"+d.getTest2());").append("\n");
        code.append("       System.out.println(\"Invoke #3: \"+d.getTest3());").append("\n");
        code.append("       System.out.println(\"Invoke #4: \"+d.getTest4());").append("\n");
        code.append("       d.getTest5();").append("\n");
        //code.append("       d.getProcess();").append("\n");
//        code.append("       if(pals.base.NodeCore.getInstance() != null)").append("\n");
//        code.append("           System.out.println(\"we can create a node...\");").append("\n");
//        code.append("       System.out.println(\"Killing JVM...\");").append("\n");
//        code.append("       System.exit(0);").append("\n");
        code.append("   }").append("\n");
        code.append("}").append("\n");
        cfm.getClassLoader().add("test.Main", code.toString());
        // -- File 1
        code = new StringBuilder();
        code.append("package test;").append("\n");
        code.append("import java.lang.Process;").append("\n");
        code.append("import java.io.IOException;").append("\n");
        code.append("import hello.Cat;").append("\n");
        code.append("public class Debug").append("\n");
        code.append("{").append("\n");
        code.append("   public String getTest() { return \"Hello :)\"; }").append("\n");
        code.append("   public static String getTest2() { return Test.helloWorld(); }").append("\n");
        code.append("   public static String getTest3() { return Cat.food(); }").append("\n");
        code.append("   public static int getTest4() { return 123; }").append("\n");
        code.append("   public static void getTest5() { while(true) System.out.println(\"infinite loop...\"); }").append("\n");
        //code.append("   public static Process getProcess() throws IOException { System.out.println(\"uh oh.\"); return Runtime.getRuntime().exec(\"ipconfig\"); }").append("\n");
        code.append("}").append("\n");
        cfm.getClassLoader().add("test.Debug", code.toString());
        // -- File 2
        code = new StringBuilder();
        code.append("package test;").append("\n");
        code.append("public class Test").append("\n");
        code.append("{").append("\n");
        code.append("   public static String helloWorld() { return \"Hello World!\";}").append("\n");
        code.append("}").append("\n");
        cfm.getClassLoader().add("test.Test", code.toString());
        // -- File 3
        code = new StringBuilder();
        code.append("package hello;").append("\n");
        code.append("public class Cat").append("\n");
        code.append("{").append("\n");
        code.append("   public static String food() { return \"om nom nom, food.\";}").append("\n");
        code.append("}").append("\n");
        cfm.getClassLoader().add("hello.Cat", code.toString());
        // -- File 4
        code = new StringBuilder();
        code.append("public class SingleNoPackage").append("\n");
        code.append("{").append("\n");
        code.append("   public static String food() { return \"om nom nom, food.\";}").append("\n");
        code.append("}").append("\n");
        cfm.getClassLoader().add("SingleNoPackage", code.toString());
        // Compile the source code
        DiagnosticCollector<JavaFileObject> diag = new DiagnosticCollector<>();
        StringWriter sw = new StringWriter();
        boolean compiled = jc.getTask(sw, cfm, diag, Arrays.asList(new String[]{"-d", "debug_output"}), null, cfm.getClassLoader().getCompilerObjects()).call();
        if(!compiled)
        {
            for(Diagnostic d : diag.getDiagnostics())
            {
                System.err.println("Compile issue ~ line: " + d.getLineNumber() + " / col: " + d.getColumnNumber() + " / message:" + d.getMessage(Locale.getDefault()) + " / code: '" + d.getCode()+"'");
            }
        }
        System.out.println("[DEBUG TEST] SW output:");
        System.out.println(sw.toString());
        System.out.println("[DEBUG TEST] COMPILED: "+compiled);
        
        read();
    }
    public static void read() throws MalformedURLException
    {
        // Create class-loader
        File dir = new File("debug_output");
        URLClassLoader cl = new URLClassLoader(new URL[]{dir.toURI().toURL()});
        System.out.println("[DEBUG TEST] Invoking...");
        // Invoke code
        try
        {
            cl.loadClass("test.Main").getMethod("main", int.class, double.class).invoke(null, 123, 456.6789);
        }
        catch(ClassNotFoundException | NoClassDefFoundError | ClassFormatError | InvocationTargetException | IllegalAccessException | NoSuchMethodException ex)
        {
            System.err.println(ex.getMessage());
        }
    }
}
