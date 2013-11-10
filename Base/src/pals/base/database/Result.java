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
    private ResultSet rs;               // The result-set of data from the query.
    private PreparedStatement ps;       // The underlying prepared-statement.
    // Methods - Constructors **************************************************
    protected Result(ResultSet rs, PreparedStatement ps)
    {
        this.rs = rs;
        this.ps = ps;
    }
    // Methods - Accessors *****************************************************
    public ResultSet getResultSet()
    {
        return this.rs;
    }
    public PreparedStatement getPreparedStatement()
    {
        return this.ps;
    }
    // Methods *****************************************************************
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
