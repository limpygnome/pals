package pals.plugins.handlers.defaultqch.data;

import java.io.Serializable;

public class CodeError implements Serializable
{
    static final long serialVersionUID = 1L;
    // Fields *************************************************************8
    public String message;
    public int line, col;
    // Methods - Constructors **********************************************
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
