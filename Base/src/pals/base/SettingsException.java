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
package pals.base;

/**
 * An exception thrown by the Settings class for any errors.
 * 
 * @version 1.0
 */
public class SettingsException extends Exception
{
    // Enums *******************************************************************
    /**
     * The type of exception.
     * 
     * @since 1.0
     */
    public enum Type
    {
        /**
         * Indicates the XML from the specified path for the configuration could
         * not be loaded.
         * 
         * @since 1.0
         */
        FailedToLoad,
        /**
         * Indicates the XML could not be parsed.
         * 
         * @since 1.0
         */
        FailedToParse,
        /**
         * Indicates a setting could not be loaded because of an invalid
         * setting.
         * 
         * @since 1.0
         */
        FailedToParse_InvalidSetting,
        /**
         * Indicates a setting with the same path existed.
         * 
         * @since 1.0
         */
        FailedToParse_DuplicateSetting,
        /**
         * Indicates a problem occurred building the XML for saving the
         * settings.
         * 
         * @since 1.0
         */
        FailedToSave_Build,
        /**
         * Indicates a problem occurred saving the XML, of settings, to file.
         * 
         * @since 1.0
         */
        FailedToSave_File,
        /**
         * Indicates the collection is read-only.
         * 
         * @since 1.0
         */
        CollectionReadOnly,
        /**
         * Indicates the node attempted to be retrieved is missing/not in the
         * collection.
         * 
         * @since 1.0
         */
        MissingNode
    }
    // Fields ******************************************************************
    private final Type type;
    // Methods - Constructors **************************************************
    /**
     * Constructs a new settings exception.
     * 
     * @param type The type of exception.
     * @param rootCause The root exception; can be null.
     * @since 1.0
     */
    public SettingsException(Type type, Throwable rootCause)
    {
        super(rootCause);
        this.type = type;
    }
    // Methods - Accessors *****************************************************
    /**
     * The type of exception.
     * 
     * @return Refer to {@link Type}
     * @since 1.0
     */
    public Type getExceptionType()
    {
        return type;
    }
}
