package pals.javasandbox;

import java.io.File;
import java.io.IOException;
import java.security.Permission;

/**
 * Responsible for further protection against potentially buggy and malicious
 * code.
 * 
 * At present, this only allows I/O within the same directory as the classes.
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
            if((path.startsWith("/") || path.startsWith("\\")) && path.length() > 1)
                path = path.substring(1);
            String fPath = new File(path).getCanonicalPath().replace("\\", "/");
            if(!fPath.startsWith(basePath+"/") && !fPath.equals(basePath))
                throw new SecurityException("Restricted path.");
        }
        catch(IOException ex)
        {
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
    public void checkPermission(Permission p)
    {
        switch(p.getClass().getName())
        {
            case "java.io.FilePermission":
                switch(p.getActions())
                {
                    case "read":
                        checkPathSafe(p.getName());
                        break;
                }
                break;
            default:            // Unhandled permission - deny!
                throw new SecurityException();
        }
    }
}
