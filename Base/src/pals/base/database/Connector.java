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
 * 
 * @version 1.0
 */
public abstract class Connector
{
    // Fields ******************************************************************
    /**
     * The underlying JDBC connection.
     * 
     * @since 1.0
     */
    protected Connection connection;
    // Methods - Connection ****************************************************
    /**
     * Connects to the database.
     * @throws DatabaseException Thrown if a database exception occurs with the
     * connector.
     * 
     * @since 1.0
     */
    public abstract void connect()                                              throws DatabaseException;
    /**
     * Disconnects from the database.
     * 
     * @since 1.0
     */
    public void disconnect()
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
                // Do nothing...
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
     * @since 1.0
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
     * @since 1.0
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
     * @since 1.0
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
     * @since 1.0
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
     * @since 1.0
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
     * @since 1.0
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
     * @since 1.0
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
     * Used to exclusively lock a table - from anything, including reads.
     * 
     * @param table The table being locked.
     * @param inTransaction Used to specify if the connector is currently
     * within a transaction. If you are not in a transaction, this will begin
     * a transaction before the lock. Invoking tableUnlock will commit the
     * transaction and thus release the lock.
     * @throws DatabaseException Thrown if an issue occurs.
     * @since 1.0
     */
    public void tableLock(String table, boolean inTransaction) throws DatabaseException
    {
        throw new IllegalStateException("Not implemented for this connector.");
    }
    /**
     * Unlocks a table by committing the transaction created by tableLock, if
     * the current connection was not in a transaction already.
     * 
     * @param inTransaction Indicates if the connector is within a
     * transaction. If this was false for tableLock, this should be false
     * again.
     * @throws DatabaseException Thrown if an issue occurs.
     * @since 1.0
     */
    public void tableUnlock(boolean inTransaction) throws DatabaseException
    {
        throw new IllegalStateException("Not implemented for this connector.");
    }
    /**
     * The type of connector, for anonymously identifying different connectors.
     * It may be possible for different connectors to use the same number,
     * therefore it is the responsibility of the user installing plugins to
     * check documentation.
     * 
     * This may be used to implement RDMS-specific SQL.
     * @return Any possible integer or 0 if unknown/not-specified/generic.
     * @since 1.0
     */
    public abstract int getConnectorType();
}
