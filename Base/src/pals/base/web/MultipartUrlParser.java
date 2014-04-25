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
    Authors:    Marcus Craske           <limpygnome@gmail.com>
    ----------------------------------------------------------------------------
*/
package pals.base.web;

/**
 * Allows dynamic relative paths to be easily parsed; performs repetitive tasks
 * such as index and null-pointer protection, as well as parsing to data-types.
 * 
 * @version 1.0
 */
public class MultipartUrlParser
{
    // Fields ******************************************************************
    private String[] data;  // Split of folders of relative URL.
    // Methods - Constructors **************************************************
    /**
     * Creates a new parser.
     * 
     * @param data The data for the current request.
     * @since 1.0
     */
    public MultipartUrlParser(WebRequestData data)
    {
        this(data.getRequestData().getRelativeUrl());
    }
    /**
     * Creates a new parser.
     * 
     * @param url The URL of the current request.
     * @since 1.0
     */
    public MultipartUrlParser(String url)
    {
        // Perform cleaning
        if(url == null)
            url = "";
        else
        {
            if(url.startsWith("/") && url.length()>1)
                url = url.substring(1);
            if(url.endsWith("/") && url.length()>1)
                url = url.substring(0, url.length()-1);
        }
        // Split parts
        this.data = url.split("/");
    }
    // Methods - Parsing *******************************************************
    /**
     * Parses an element to an integer.
     * 
     * @param index The index of the directory to parse.
     * @param alt The alternate value to return if the index is invalid.
     * @return The parsed value or the alt parameter.
     * @since 1.0
     */
    public int parseInt(int index, int alt)
    {
        String s = getPart(index);
        if(s == null)
            return alt;
        try
        {
            return Integer.parseInt(s);
        }
        catch(NumberFormatException ex)
        {
            return alt;
        }
    }
    // Methods - Accessors *****************************************************
    /**
     * Retrieves a part of the URL at an index.
     * 
     * @param index The index of the directory to retrieve.
     * @return The data at the index; either a string or null (converted to null
     * if empty-string).
     * @since 1.0
     */
    public String getPart(int index)
    {
        return data.length >= index+1 ? (data[index].length() > 0 ? data[index] : null) : null;
    }
}
