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
package pals.javasandbox;

import java.io.File;
import java.io.IOException;
import java.security.Permission;

/**
 * Responsible for further protection against potentially buggy and malicious
 * code.
 * 
 * At present, this only allows I/O within the same directory as the classes
 * and for the JVM to exit.
 * 
 * Note:
 * - Any class-loaders must be made after the security-manager is enforced,
 *   using System.setSecurityManager(...).
 */
public class SandboxSecurityManager extends SecurityManager
{
    // Fields ******************************************************************
    private final String    basePath;
    // Methods - Constructors **************************************************
    public SandboxSecurityManager(String basePath) throws IOException
    {
        this.basePath = basePath.replace("\\", "/");
    }
    // Methods - Security ******************************************************
    @Override
    public void checkRead(String path)
    {
        checkPathSafe(path);
    }
    @Override
    public void checkWrite(String path)
    {
        checkPathSafe(path);
    }
    private void checkPathSafe(String path)
    {
        if(path != null)return;
        try
        {
            // Build path data
            String fPath = new File(path).getCanonicalPath().replace("\\", "/");
            // Output debugging information
            if(JavaSandbox.modeDebug)
                System.out.println("[DEBUG] Security-manager - checking path - p: '"+path+"', fp: "+fPath+"', bp: '"+basePath+"'.");
            // Perform check
            if(!fPath.startsWith(basePath+"/") && !fPath.equals(basePath))
            {
                // Check the path is not a lib in the jre
                if(!fPath.startsWith(System.getProperty("java.home").replace("\\", "/")+"/lib"))
                {
                    System.out.println("[DEBUG] Path disallowed ~ '"+path+"'.");
                    System.out.println("'"+fPath+"' ~ '"+System.getProperty("java.home")+"/lib"+"'");
                    throw new SecurityException("Restricted path '"+path+"'.");
                }
            }
            else if(JavaSandbox.modeDebug)
                System.out.println("[DEBUG] Path allowed ~ '"+path+"'.");
        }
        catch(IOException ex)
        {
            if(JavaSandbox.modeDebug)
                JavaSandbox.printDebugData(ex);
            throw new SecurityException("Could not process path; considered restricted.");
        }
    }
    @Override
    public void checkCreateClassLoader()
    {
        // Allow class-loaders
        // -- Our own class-loader will be needed.
    }
    @Override
    public void checkExit(int i)
    {
        // Allowed - the user may want to terminate their program or the
        // thread-watcher may want to kill the JVM due to excessive
        // computation.
    }
    @Override
    public void checkPermission(Permission p)
    {
        if(JavaSandbox.modeDebug)
            System.out.println("[DEBUG] Security-manager permission check data: '"+p.toString()+"' ~ name: '"+p.getName()+"', action(s) '"+p.getActions()+"', class '"+p.getClass().getName()+"'.");
        switch(p.getClass().getName())
        {
            case "java.io.FilePermission":
            {
                checkPathSafe(p.getName());
                return;
            }
            case "java.util.PropertyPermission":
            {
                switch(p.getName())
                {
                    case "line.separator":
                        switch(p.getActions())
                        {
                            case "read":        return;
                        }
                        break;
                    case "java.protocol.handler.pkgs":
                        switch(p.getActions())
                        {
                            case "read":        return;
                        }
                        break;
                    case "user.dir":
                        // Working-directory - safe
                        // -- http://docs.oracle.com/javase/tutorial/essential/environment/sysprop.html
                        return;
                    case "user.language.format":
                    case "user.script.format":
                    case "user.country.format":
                    case "user.variant.format":
                    case "java.home": // Home directory ~ sketchy allowance, but required by /libs
                        return;
                }
                break;
            }
            // All of these below are required for Scanner
            // -- Potentially dangerous:
            // -- -- suppressAccessChecks ~ http://docs.oracle.com/javase/6/docs/api/java/lang/reflect/ReflectPermission.html
            // -- -- specifyStreamHandler - http://download.java.net/jdk7/archive/b123/docs/api/java/net/NetPermission.html
            // -- This is not recommended by the API, but it's required.
            case "java.net.NetPermission":
            {
                switch(p.getName())
                {
                    case "specifyStreamHandler":
                        return;
                }
            }
            case "java.lang.RuntimePermission":
            {
                switch(p.getName())
                {
                    case "accessClassInPackage.sun.text.resources":
                        return;
                }
            }
            case "java.lang.reflect.ReflectPermission":
            {
                switch(p.getName())
                {
                    case "suppressAccessChecks":
                        return;
                }
            }
        }
        throw new SecurityException("Attempted disallowed operation; permission: "+p.getClass().getName()+"/"+p.getName());
    }
}
