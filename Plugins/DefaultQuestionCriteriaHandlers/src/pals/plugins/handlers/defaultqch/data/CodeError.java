package pals.plugins.handlers.defaultqch.data;

import java.io.Serializable;

public class CodeError implements Serializable
{
    static final long serialVersionUID = 1L;
    // Fields *************************************************************8
    private String  message;
    private int     line,
                    col;
    // Methods - Constructors **********************************************
    public CodeError()
    {
        this.message = "No error message defined.";
        this.line = -1;
        this.col = -1;
    }
    public CodeError(String message, int line, int col)
    {
        this.message = message;
        this.line = line;
        this.col = col;
    }
    // Methods - Accessors *************************************************
    public String getMessage()
    {
        return message;
    }
    public int getLine()
    {
        return line;
    }
    public int getCol()
    {
        return col;
    }
}
