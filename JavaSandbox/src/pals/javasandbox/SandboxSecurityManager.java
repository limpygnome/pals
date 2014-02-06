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
                System.out.println("[DEBUG] Path disallowed ~ '"+path+"'.");
                throw new SecurityException("Restricted path '"+path+"'.");
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
                }
                break;
            }
        }
        throw new SecurityException("Attempted disallowed operation; permission: "+p.getClass().getName()+"/"+p.getName());
    }
}
