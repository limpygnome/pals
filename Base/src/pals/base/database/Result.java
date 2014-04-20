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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * A class for handling the results from the execution of a query.
 * 
 * @version 1.0
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
     * Gets the ResultSet from the query.
     * 
     * @return The result set.
     * @since 1.0
     */
    public ResultSet getResultSet()
    {
        return this.rs;
    }
    /**
     * Gets the prepared-statement used for the query.
     * 
     * @return The prepared statement.
     * @since 1.0
     */
    public PreparedStatement getPreparedStatement()
    {
        return this.ps;
    }
    // Methods *****************************************************************
    /**
     * Indicates if a column exists in the result.
     * 
     * @param column The column name to check.
     * @return True = exists, false = does not exist.
     * @since 1.0
     */
    public boolean contains(String column)
    {
        try
        {
            ResultSetMetaData md = rs.getMetaData();
            for(int i = 1; i <= md.getColumnCount(); i++)
            {
                if(md.getColumnLabel(i).equals(column))
                    return true;   
            }
            return false;
        }
        catch(SQLException ex)
        {
            return false;
        }
    }
    /**
     * Moves to the next tuple/row in the result.
     * 
     * @return True = new row available, false = end of result.
     * @throws DatabaseException Thrown if a database error occurs.
     * @since 1.0
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
     * 
     * @param <T> The data-type of the value to read.
     * @param attributeName The attribute to read.
     * @return The value of the specified attribute.
     * @throws DatabaseException Thrown if a database error occurs.
     * @since 1.0
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
     * @since 1.0
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
