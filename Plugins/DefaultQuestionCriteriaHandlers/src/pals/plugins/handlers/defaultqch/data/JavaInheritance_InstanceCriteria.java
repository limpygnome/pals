

package pals.plugins.handlers.defaultqch.data;

import java.io.Serializable;

/**
 *
 * @author limpygnome
 */
public class JavaInheritance_InstanceCriteria implements Serializable
{
    static final long serialVersionUID = 1L;
    
    // Enums *******************************************************************
    public enum Status
    {
        ClassNotFound,
        IncorrectExtend,
        Correct
    }
    // Fields ******************************************************************
    private Status      status;
    private String[]    classesNotImplemented;
    // Methods - Constructors **************************************************
    public JavaInheritance_InstanceCriteria()
    {
        this.status = Status.ClassNotFound;
        this.classesNotImplemented = new String[0];
    }
    // Methods - Mutators ******************************************************
    
    public void setClassesNotImplemented(String[] classesNotImplemented)
    {
        this.classesNotImplemented = classesNotImplemented;
    }

    public void setStatus(Status status)
    {
        this.status = status;
    }
    // Methods - Accessors *****************************************************
    
    public Status getStatus()
    {
        return status;
    }

    public String[] getClassesNotImplemented()
    {
        return classesNotImplemented;
    }
}
