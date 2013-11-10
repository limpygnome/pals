package pals.base.database;

public class DatabaseException extends Exception
{
    // Enums *******************************************************************
    public enum Type
    {
        /**
         * A connection-initiation related exception.
         */
        ConnectionFailure,
        /**
         * Indicates a connection has already been made; thrown when
         * connect is called again without a call to disconnect.
         */
        ConnectionAlreadyEstablished,
        /**
         * An exception occurred whilst disconnecting from a database.
         */
        DisconnectionFailure,
        /**
         * A connection related exception, after an initial connection.
         */
        ConnectionException,
        /**
         * An exception generated from generating/preparing/building a query.
         */
        QueryCreationException,
        /**
         * An exception generated from setting a substituted value when building
         * a query.
         */
        QueryCreationInvalidValueException,
        /**
         * An exception generated from a query.
         */
        QueryException,
        /**
         * An exception from disposing the result of a query.
         */
        QueryDisposeException,
        /**
         * An unknown reason.
         */
        Unknown
    };
    // Methods - Fields ********************************************************
    private final   Type    type;
    // Methods - Constructors **************************************************
    public DatabaseException()
    {
        super();
        this.type = Type.Unknown;
    }
    public DatabaseException(Throwable rootException)
    {
        super(rootException);
        this.type = Type.Unknown;
    }
    public DatabaseException(Type type)
    {
        super();
        this.type = type;
    }
    public DatabaseException(Type type, Throwable rootException)
    {
        super(rootException);
        this.type = type;
    }
    // Methods - Accessors *****************************************************
    /**
     * The type of database exception.
     * @return Type.
     */
    public Type getType()
    {
        return this.type;
    }
}
