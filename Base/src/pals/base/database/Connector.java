package pals.base.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * The base class for handling database connectivity.
 * 
 * This is implemented by connectors for a specific RDBMS, allowing
 * cross-database communication using standard SQL or switching on the
 * getConnectorType method.
 */
public abstract class Connector
{
    // Fields ******************************************************************
    protected Connection connection;      // The underlying JDBC connection.
    // Methods - Connection ****************************************************
    /**
     * Connects to the database.
     * @throws DatabaseException Thrown if a database exception occurs with the
     * connector.
     */
    public abstract void connect()                                              throws DatabaseException;
    /**
     * Disconnects from the database.
     * @throws DatabaseException Thrown if a database exception occurs with the
     * connector.
     */
    public void disconnect()                                                    throws DatabaseException
    {
        if(this.connection != null)
        {
            try
            {
                this.connection.close();
                this.connection = null;
            }
            catch(SQLException ex)
            {
                throw new DatabaseException(DatabaseException.Type.DisconnectionFailure, ex);
            }
        }
    }
    // Methods - Queries *******************************************************
    /**
     * Prepares a query with escaped values.
     * 
     * @param query Query, with ? for substituted values.
     * @param values The values substituted in the query.
     * @return An instance of PreparedStatement ready for execution.
     * @throws DatabaseException Thrown if the prepared-statement cannot be
     * created.
     */
    public PreparedStatement prepare(String query, Object... values)            throws DatabaseException
    {
        PreparedStatement ps = null;
        // Create an instance of a prepared-statement
        try
        {
            ps = connection.prepareStatement(query);
        }
        catch(SQLException ex)
        {
            throw new DatabaseException(DatabaseException.Type.QueryCreationException, ex);
        }
        // Add substituted values
        Object obj;
        Class type;
        for(int i = 1; i <= values.length; i++)
        {
            // Non-zero index when setting substituted values...
            obj = values[i-1];
            try
            {
                ps.setObject(i, obj);
            }
            catch(SQLException ex)
            {
                throw new DatabaseException(DatabaseException.Type.QueryCreationInvalidValueException, ex);
            }
        }
        return ps;
    }
    /**
     * Executes a query.
     * 
     * @param query The query to be executed. '?' without quotations should be
     * used in places where a value is specified. The nth-? corresponds to the
     * nth value in the values parameter passed.
     * @param values The values for substitution in the query.
     * @throws DatabaseException Thrown if a database exception occurs with the
     * connector.
     */
    public void execute(String query, Object... values)                         throws DatabaseException
    {
        PreparedStatement ps = prepare(query, values);
        execute(ps);
    }
    /**
     * Executes a prepared statement.
     * 
     * @param ps The prepared statement to be executed.
     * @throws DatabaseException Thrown if a database exception occurs with the
     * connector.
     */
    public void execute(PreparedStatement ps)                                   throws DatabaseException
    {
        try
        {
            ps.execute();
            ps.close();
        }
        catch(SQLException ex)
        {
            throw new DatabaseException(DatabaseException.Type.QueryException, ex);
        }
    }
    /**
     * Executes a query with a scalar value returned.
     * 
     * @param query The query to be executed. '?' without quotations should be
     * used in places where a value is specified. The nth-? corresponds to the
     * nth value in the values parameter passed.
     * @param values The values for substitution in the query.
     * @return Single value returned from the query; may be null.
     * @throws DatabaseException Thrown if a database exception occurs with the
     * connector.
     */
    public Object executeScalar(String query, Object... values)                 throws DatabaseException
    {
        PreparedStatement ps = prepare(query, values);
        return executeScalar(ps);
    }
    /**
     * Executes a prepared statement with a scalar value returned.
     * 
     * @param ps The prepared-statement to be executed.
     * @return Single value returned from the query; may be null.
     * @throws DatabaseException Thrown if a database exception occurs with the
     * connector.
     */
    public Object executeScalar(PreparedStatement ps)                           throws DatabaseException
    {
        try
        {
            ResultSet rs = ps.executeQuery();
            Object t = rs.next() ? rs.getObject(1) : null;
            rs.close();
            ps.close();
            return t;
        }
        catch(SQLException ex)
        {
            throw new DatabaseException(DatabaseException.Type.QueryException, ex);
        }
    }
    /**
     * Executes and returns the result from the query.
     * 
     * @param query The query to be executed. '?' without quotations should be
     * used in places where a value is specified. The nth-? corresponds to the
     * nth value in the values parameter passed.
     * @param values The values for substitution in the query.
     * @return The result from the query.
     * @throws DatabaseException Thrown if a database exception occurs with the
     * connector.
     */
    public Result read(String query, Object... values)                          throws DatabaseException
    {
        PreparedStatement ps = prepare(query, values);
        return read(ps);
    }
    /**
     * Executes a prepared-statement and returns the result.
     * @param ps The prepared-statement to be executed.
     * @return The result from the query.
     * @throws DatabaseException Thrown if a database exception occurs with the
     * connector.
     */
    public Result read(PreparedStatement ps)                                    throws DatabaseException
    {
        try
        {
            return new Result(ps.executeQuery(), ps);
        }
        catch(SQLException ex)
        {
            throw new DatabaseException(DatabaseException.Type.QueryException, ex);
        }
    }
    /**
     * The type of connector, for anonymously identifying different connectors.
     * It may be possible for different connectors to use the same number,
     * therefore it is the responsibility of the user installing plugins to
     * check documentation.
     * 
     * This may be used to implement RDMS-specific SQL.
     * @return Any possible integer or 0 if unknown/not-specified/generic.
     */
    public abstract int getConnectorType();
}
