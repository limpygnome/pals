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
package pals.javasandbox.parsers;

import pals.javasandbox.ParsedArgument;

/**
 * Parses a character argument.
 */
public class PA_Char implements Parser
{
    @Override
    public void parse(ParsedArgument pa, String[] value, boolean array)
    {
        pa.setArgClass(array ? char[].class : char.class);
        if(array)
        {
            char[] data = new char[value.length];
            for(int i = 0; i < value.length; i++)
                data[i] = parse(value[i]);
            pa.setArgValue(data);
        }
        else
            pa.setArgValue(parse(value[0]));
    }
    private char parse(String data)
    {
        return data.length() == 0 ? data.charAt(0) : (char)Integer.parseInt(data);
    }
    
    @Override
    public Object[] convert(Object raw)
    {
        char[] arr = (char[])raw;
        Object[] r = new Object[arr.length];
        for(int i = 0; i < arr.length; i++)
            r[i] = Character.valueOf(arr[i]);
        return r;
    }
}
