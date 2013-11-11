package pals.base.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A class for handling the results from the execution of a query.
 */
public class Result
{
    // Fields ******************************************************************
    private final ResultSet         rs;     // The result-set of data from the query.
    private final PreparedStatement ps;     // The underlying prepared-statement.
    // Methods - Constructors **************************************************
    protected Result(ResultSet rs, PreparedStatement ps)
    {
        this.rs = rs;
        this.ps = ps;
    }
    // Methods - Accessors *****************************************************
    /**
     * @return Gets the ResultSet from the query.
     */
    public ResultSet getResultSet()
    {
        return this.rs;
    }
    /**
     * @return Gets the prepared-statement used for the query.
     */
    public PreparedStatement getPreparedStatement()
    {
        return this.ps;
    }
    // Methods *****************************************************************
    /**
     * @return Moves to the next tuple/row in the result.
     * @throws DatabaseException Thrown if a database error occurs.
     */
    public boolean next() throws DatabaseException
    {
        try
        {
            return rs.next();
        }
        catch(SQLException ex)
        {
            throw new DatabaseException(DatabaseException.Type.QueryException, ex);
        }
    }
    /**
     * Retrieves an attribute as the specified type.
     * 
     * WARNING: this can be used dangerously, but it also makes programming
     * a lot easier. Only type-casting is applied, no type-conversion.
     * @param <T> The data-type of the value to read.
     * @param attributeName The attribute to read.
     * @return The value of the specified attribute.
     * @throws DatabaseException Thrown if a database error occurs.
     */
    public <T> T get(String attributeName) throws DatabaseException
    {
        try
        {
            return (T)rs.getObject(attributeName);
        }
        catch(SQLException ex)
        {
            throw new DatabaseException(DatabaseException.Type.QueryException, ex);
        }
    }
    /**
     * Disposes the underlying result-set and prepared-statement.
     * 
     * @throws DatabaseException Thrown if the prepared-statement cannot be
     * created.
     */
    public void dispose() throws DatabaseException
    {
        try
        {
            this.rs.close();
            this.ps.close();
        }
        catch(SQLException ex)
        {
            throw new DatabaseException(DatabaseException.Type.QueryDisposeException, ex);
        }
    }
}
