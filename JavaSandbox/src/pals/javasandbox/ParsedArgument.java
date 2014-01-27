package pals.javasandbox;

/**
 * A parsed argument.
 * 
 * Accepted types:
 * - all primitives : byte, short, int, long, float, double, boolean and char.
 */
public class ParsedArgument
{
    // Fields ******************************************************************
    private final Object    argValue;
    private final Class     argClass;
    // Methods - Constructors **************************************************
    public ParsedArgument(Object argValue, Class argClass)
    {
        this.argValue = argValue;
        this.argClass = argClass;
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The value of the argument.
     */
    public Object getArgValue()
    {
        return argValue;
    }
    /**
     * @return The class of the argument.
     */
    public Class getArgClass()
    {
        return argClass;
    }
    
    public static ParsedArgument parse(String arg) throws IllegalArgumentException
    {
        // http://docs.oracle.com/javase/tutorial/java/nutsandbolts/datatypes.html
       int se = arg.indexOf("=");
       if(se != -1 && se < arg.length() - 1)
       {
           try
           {
               String  k = arg.substring(0, se),
                       v = arg.substring(se+1);
               switch(k)
               {
                   case "byte":
                       return new ParsedArgument(Byte.parseByte(v), byte.class);
                   case "short":
                       return new ParsedArgument(Short.parseShort(v), short.class);
                   case "int":
                       return new ParsedArgument(Integer.parseInt(v), int.class);
                   case "long":
                       return new ParsedArgument(Long.parseLong(v), long.class);
                   case "float":
                       return new ParsedArgument(Float.parseFloat(v), float.class);
                   case "double":
                       return new ParsedArgument(Double.parseDouble(v), double.class);
                   case "string":
                   case "str":
                       return new ParsedArgument(v, String.class);
                   case "char":
                       if(v.length() == 1)
                           return new ParsedArgument(v.charAt(0), char.class);
                       else
                       {
                           // Attempt to parse as number
                           int value = Integer.parseInt(v);
                           return new ParsedArgument((char)value, char.class);
                       }
                   case "bool":
                   case "boolean":
                       return new ParsedArgument(Boolean.parseBoolean(v), boolean.class);
               }
           }
           catch(NumberFormatException | NullPointerException ex)
           {
           }
       }
       throw new IllegalArgumentException();
    }
}
