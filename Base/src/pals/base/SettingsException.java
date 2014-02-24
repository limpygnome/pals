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
package pals.base;

/**
 * An exception thrown by the Settings class for any errors.
 */
public class SettingsException extends Exception
{
    // Enums
    public enum Type
    {
        /**
         * Indicates the XML from the specified path for the configuration could
         * not be loaded.
         */
        FailedToLoad,
        /**
         * Indicates the XML could not be parsed.
         */
        FailedToParse,
        /**
         * Indicates a setting could not be loaded because of an invalid setting.
         */
        FailedToParse_InvalidSetting,
        /**
         * Indicates a setting with the same path existed.
         */
        FailedToParse_DuplicateSetting,
        /**
         * Indicates a problem occurred building the XML for saving the settings.
         */
        FailedToSave_Build,
        /**
         * Indicates a problem occurred saving the XML, of settings, to file.
         */
        FailedToSave_File,
        /**
         * Indicates the collection is read-only.
         */
        CollectionReadOnly,
        /**
         * Indicates the node attempted to be retrieved is missing/not in the
         * collection.
         */
        MissingNode
    }
    // Fields
    private final Type type;
    // Methods - Constructors
    public SettingsException(Type type, Throwable rootCause)
    {
        super(rootCause);
        this.type = type;
    }
    // Methods - Accessors
    /**
     * @return The type of exception.
     */
    public Type getExceptionType()
    {
        return type;
    }
}
