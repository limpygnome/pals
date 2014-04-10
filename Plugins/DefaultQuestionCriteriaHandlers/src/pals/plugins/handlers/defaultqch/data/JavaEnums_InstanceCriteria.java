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
package pals.plugins.handlers.defaultqch.data;

import java.io.Serializable;

/**
 * Stores data regarding an instance of a criteria.
 * 
 * @version 1.0
 */
public class JavaEnums_InstanceCriteria implements Serializable
{
    static final long serialVersionUID = 1L;
    
    // Enums *******************************************************************
    /**
     * The status of the criteria.
     * 
     * @since 1.0
     */
    public enum Status
    {
        /**
         * Indicates the enum could not be found.
         * 
         * @since 1.0
         */
        NotFound,
        /**
         * Indicates the class found is not an enum.
         * 
         * @since 1.0
         */
        NotEnum,
        /**
         * Indicates the enum has incorrect constants.
         * 
         * @since 1.0
         */
        Incorrect,
        /**
         * Indicates the enum is correct.
         * 
         * @since 1.0
         */
        Correct
    }
    // Fields ******************************************************************
    private Status      status;
    private String[]    valuesMissing,
                        valuesExtra;
    // Methods - Constructors **************************************************
    /**
     * Creates a new instance.
     * 
     * @since 1.0
     */
    public JavaEnums_InstanceCriteria()
    {
        this.status = Status.NotFound;
        this.valuesMissing = new String[0];
        this.valuesExtra = new String[0];
    }
    // Methods - Mutators ******************************************************
    /**
     * Sets if the status of this instance.
     * 
     * @param status The status.
     * @since 1.0
     */
    public void setStatus(Status status)
    {
        this.status = status;
    }
    /**
     * Sets the values missing.
     * 
     * @param valuesMissing Array; cannot be null.
     * @since 1.0
     */
    public void setValuesMissing(String[] valuesMissing)
    {
        this.valuesMissing = valuesMissing == null ? new String[0] : valuesMissing;
    }
    /**
     * Sets the values provided extra by the user. This should not occur when
     * extra values are allowed.
     * 
     * @param valuesExtra The extra values.
     * @since 1.0
     */
    public void setValuesExtra(String[] valuesExtra)
    {
        this.valuesExtra = valuesExtra == null ? new String[0] : valuesExtra;
    }
    // Methods - Accessors *****************************************************
    /**
     * The status of this instance of criteria.
     * 
     * @return The status.
     * @since 1.0
     */
    public Status getStatus()
    {
        return status;
    }
    /**
     * The enum values missing.
     * 
     * @return Array; can be empty.
     * @since 1.0
     */
    public String[] getValuesMissing()
    {
        return valuesMissing;
    }
    /**
     * Indicates if there are any values missing.
     * 
     * @return True = missing, false = not missing.
     * @since 1.0
     */
    public boolean isValuesMissing()
    {
        return valuesMissing.length > 0;
    }
    /**
     * The enum values the user provided extra; this is ignored when extra
     * enum values are allowed.
     * 
     * @return Array; can be empty.
     * @since 1.0
     */
    public String[] getValuesExtra()
    {
        return valuesExtra;
    }
    /**
     * Indicates if there are any extra values the user provided.
     * 
     * @return True = extra, false = no extra.
     * @since 1.0
     */
    public boolean isValuesExtra()
    {
        return valuesExtra.length > 0;
    }
}
