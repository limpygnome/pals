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
package pals.base.database;

/**
 * Thrown by any class extending {@link Connector} when an exception occurs.
 * 
 * @version 1.0
 */
public class DatabaseException extends Exception
{
    // Enums *******************************************************************
    public enum Type
    {
        /**
         * A connection-initiation related exception.
         * 
         * @since 1.0
         */
        ConnectionFailure,
        /**
         * Indicates a connection has already been made; thrown when
         * connect is called again without a call to disconnect.
         * 
         * @since 1.0
         */
        ConnectionAlreadyEstablished,
        /**
         * A connection related exception, after an initial connection.
         * 
         * @since 1.0
         */
        ConnectionException,
        /**
         * An exception generated from generating/preparing/building a query.
         * 
         * @since 1.0
         */
        QueryCreationException,
        /**
         * An exception generated from setting a substituted value when building
         * a query.
         * 
         * @since 1.0
         */
        QueryCreationInvalidValueException,
        /**
         * An exception generated from a query.
         * 
         * @since 1.0
         */
        QueryException,
        /**
         * An exception from disposing the result of a query.
         * 
         * @since 1.0
         */
        QueryDisposeException,
        /**
         * An unknown reason.
         * 
         * @since 1.0
         */
        Unknown
    };
    // Methods - Fields ********************************************************
    private final   Type    type;
    // Methods - Constructors **************************************************
    /**
     * Constructs a new instance.
     * 
     * @since 1.0
     */
    public DatabaseException()
    {
        super();
        this.type = Type.Unknown;
    }
    /**
     * Constructs a new instance.
     * 
     * @param rootException The underlying exception thrown.
     * @since 1.0
     */
    public DatabaseException(Throwable rootException)
    {
        super(rootException);
        this.type = Type.Unknown;
    }
    /**
     * Constructs a new instance.
     * 
     * @param type The type of exception.
     * @since 1.0
     */
    public DatabaseException(Type type)
    {
        super();
        this.type = type;
    }
    /**
     * Constructs a new instance.
     * 
     * @param type The type of exception.
     * @param rootException The underlying exception.
     * @since 1.0
     */
    public DatabaseException(Type type, Throwable rootException)
    {
        super(rootException);
        this.type = type;
    }
    // Methods - Accessors *****************************************************
    /**
     * The type of database exception.
     * 
     * @return The {@link Type} of exception.
     * @since 1.0
     */
    public Type getType()
    {
        return this.type;
    }
}
