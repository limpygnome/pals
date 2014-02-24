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

import pals.javasandbox.parsers.PA_Boolean;
import pals.javasandbox.parsers.PA_Byte;
import pals.javasandbox.parsers.PA_Char;
import pals.javasandbox.parsers.PA_Double;
import pals.javasandbox.parsers.PA_Float;
import pals.javasandbox.parsers.PA_Integer;
import pals.javasandbox.parsers.PA_Long;
import pals.javasandbox.parsers.PA_Short;
import pals.javasandbox.parsers.PA_String;
import pals.javasandbox.parsers.Parser;

/**
 * A parsed argument.
 * 
 * Accepted types:
 * - all primitives : byte, short, int, long, float, double, boolean and char.
 * - object type: string
 * 
 * Argument format:
 * <type>=<value>
 * 
 * Values which are arrays should contain commas; therefore strings cannot have
 * commas, or they will be treated as a string array.
 */
public class ParsedArgument
{
    // Fields ******************************************************************
    private Object  argValue;
    private Class   argClass;
    // Methods - Constructors **************************************************
    private ParsedArgument()
    {
        this.argValue = null;
        this.argClass = null;
    }
    // Methods - Mutators ******************************************************
    /**
     * @param argValue Sets the parsed value.
     */
    public void setArgValue(Object argValue)
    {
        this.argValue = argValue;
    }
    /**
     * @param argClass Sets the parsed class.
     */
    public void setArgClass(Class argClass)
    {
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
    /**
     * Parses an argument.
     * 
     * @param arg The argument data; refer to class documentation.
     * @return A parsed argument.
     * @throws IllegalArgumentException Thrown if the argument data is
     * invalid.
     */
    public static ParsedArgument parse(String arg) throws IllegalArgumentException
    {
        // http://docs.oracle.com/javase/tutorial/java/nutsandbolts/datatypes.html
       int se = arg.indexOf("=");
       if(se != -1)
       {
           try
           {
               // Prepare values
               boolean array = false;
               String   k = arg.substring(0, se).toLowerCase();
               String[] v = se == arg.length()-1 ? new String[0] : arg.substring(se+1).split(",");
               if(k.length() > 2 && k.endsWith("[]"))
               {
                   array = true;
                   k = k.substring(0, k.length()-2);
               }
               // Create parser
               Parser p;
               switch(k)
               {
                   case "byte":
                       p = new PA_Byte();
                       break;
                   case "short":
                       p = new PA_Short();
                       break;
                   case "int":
                       p = new PA_Integer();
                       break;
                   case "long":
                       p = new PA_Long();
                       break;
                   case "float":
                       p = new PA_Float();
                       break;
                   case "double":
                       p = new PA_Double();
                       break;
                   case "string":
                   case "str":
                       p = new PA_String();
                       break;
                   case "char":
                       p = new PA_Char();
                       break;
                   case "bool":
                   case "boolean":
                       p = new PA_Boolean();
                       break;
                   default:
                       throw new IllegalArgumentException("Invalid sandbox parsed argument type '"+k+"'.");
               }
               // Parse data
               ParsedArgument pa = new ParsedArgument();
               p.parse(pa, v, array);
               return pa;
           }
           catch(NumberFormatException | NullPointerException ex)
           {
               throw new IllegalArgumentException("Invalid sandbox argument value(s).");
           }
       }
       throw new IllegalArgumentException("Invalid sandbox argument(s).");
    }
}
