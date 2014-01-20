package pals.plugins.handlers.defaultqch.java;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
        //System.setSecurityManager(new CompilerSecurityManager());
        
        JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
        if(jc == null)
        {
            System.err.println("[DEBUG TEST] Cannot access JVM.");
            return;
        }
        CompilerFileManager cfm = new CompilerFileManager(jc.getStandardFileManager(null, null, null), "temp", true, false);
        
        
        StringBuilder code;
        
        // Add file 0
        code = new StringBuilder();
        code.append("package test;").append("\n");
        code.append("import java.io.IOException;").append("\n");
        code.append("public class Main").append("\n");
        code.append("{").append("\n");
        code.append("   public static void main() throws IOException").append("\n");
        code.append("   {").append("\n");
        code.append("       System.out.println(\"test main!\");").append("\n");
        code.append("       Debug d = new Debug();").append("\n");
        code.append("       System.out.println(\"Invoke #1: \"+d.getTest());").append("\n");
        code.append("       System.out.println(\"Invoke #2: \"+d.getTest2());").append("\n");
        code.append("       System.out.println(\"Invoke #3: \"+d.getTest3());").append("\n");
        code.append("       System.out.println(\"Invoke #4: \"+d.getTest4());").append("\n");
        //code.append("       d.getProcess();").append("\n");
//        code.append("       if(pals.base.NodeCore.getInstance() != null)").append("\n");
//        code.append("           System.out.println(\"we can create a node...\");").append("\n");
        code.append("       System.out.println(\"Killing JVM...\");").append("\n");
        code.append("       System.exit(0);").append("\n");
        code.append("   }").append("\n");
        code.append("}").append("\n");
        cfm.getClassLoader().add("test.Main", code.toString());
        
        // Add file 1
        code = new StringBuilder();
        code.append("package test;").append("\n");
        code.append("import java.lang.Process;").append("\n");
        code.append("import java.io.IOException;").append("\n");
        code.append("import hello.Cat;").append("\n");
        code.append("public class Debug").append("\n");
        code.append("{").append("\n");
        code.append("   public String getTest() { return \"hai\"; }").append("\n");
        code.append("   public static String getTest2() { return Test.helloWorld(); }").append("\n");
        code.append("   public static String getTest3() { return Cat.food(); }").append("\n");
        code.append("   public static int getTest4() { return 123; }").append("\n");
        //code.append("   public static Process getProcess() throws IOException { System.out.println(\"uh oh.\"); return Runtime.getRuntime().exec(\"ipconfig\"); }").append("\n");
        code.append("}").append("\n");
        cfm.getClassLoader().add("test.Debug", code.toString());
        
        // Add file 2
        code = new StringBuilder();
        code.append("package test;").append("\n");
        code.append("public class Test").append("\n");
        code.append("{").append("\n");
        code.append("   public static String helloWorld() { return \"hai\";}").append("\n");
        code.append("}").append("\n");
        cfm.getClassLoader().add("test.Test", code.toString());
        
        // Add file 3
        code = new StringBuilder();
        code.append("package hello;").append("\n");
        code.append("public class Cat").append("\n");
        code.append("{").append("\n");
        code.append("   public static String food() { return \"om nom nom, food.\";}").append("\n");
        code.append("}").append("\n");
        cfm.getClassLoader().add("hello.Cat", code.toString());
        
        // Add white-listing
        cfm.getClassLoader().whiteListAdd("java.lang.Object");
        cfm.getClassLoader().whiteListAdd("java.lang.String");
        cfm.getClassLoader().whiteListAdd("java.lang.System");
        cfm.getClassLoader().whiteListAdd("java.io.PrintStream");
        cfm.getClassLoader().whiteListAdd("java.lang.StringBuilder");
        cfm.getClassLoader().whiteListAdd("java.io.IOException");
        cfm.getClassLoader().whiteListAdd("pals.base.NodeCore");
        
        // Compile the source code
        DiagnosticCollector<JavaFileObject> diag = new DiagnosticCollector<>();
        StringWriter sw = new StringWriter();
        boolean compiled = jc.getTask(sw, cfm, diag, Arrays.asList(new String[]{"-d", "temp"}), null, cfm.getClassLoader().getCompilerObjects()).call();
        if(!compiled)
        {
            for(Diagnostic d : diag.getDiagnostics())
            {
                System.err.println("diag issue ~ line: " + d.getLineNumber() + " / col: " + d.getColumnNumber() + " / message:" + d.getMessage(Locale.getDefault()) + " / code: '" + d.getCode()+"'");
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
        File dir = new File("temp");
        URLClassLoader cl = new URLClassLoader(new URL[]{dir.toURI().toURL()});
        System.out.println("[DEBUG TEST] Invoking...");
        // Invoke code
        try
        {
//            Object test = cfm.getClassLoader().loadClass("test.Debug").newInstance();
//            System.out.println(test.getClass().getMethod("getTest").invoke(test));
//            System.out.println(test.getClass().getMethod("getTest2").invoke(null));
//            System.out.println(test.getClass().getMethod("getTest3").invoke(null));
//            System.out.println(test.getClass().getMethod("getTest4").invoke(null));
//            System.out.println("[DEBUG TEST] Finished invoking without error.");
            cl.loadClass("test.Main").getMethod("main").invoke(null);
        }
        catch(ClassNotFoundException | NoClassDefFoundError | ClassFormatError | InvocationTargetException | IllegalAccessException | NoSuchMethodException ex)
        {
            if(ex.getCause() != null)
            {
                if(ex.getCause() instanceof WhitelistException)
                    System.err.println("[DEBUG TEST] RESTRICTED CLASS: '"+((WhitelistException)ex.getCause()).getClassName()+"'");
                else
                    System.err.println("[DEBUG TEST] Not instanceof.");
                System.err.println("[DEBUG TEST] CAUSE: "+ex.getCause().getMessage());
            }
            else
                System.err.println(ex.getMessage());
        }
    }
}
