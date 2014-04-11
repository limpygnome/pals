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
package pals.base.database.connectors;

import static org.junit.Assert.*;
import org.junit.Test;
import pals.TestWithCore;
import pals.base.database.DatabaseException;

/**
 * Tests {@link Postgres}.
 * 
 * @version 1.0
 */
public class PostgresTest
{
    private final String HOST = "127.0.0.1";
    private final int PORT = 5432;
    private final String DB = "pals";
    private final String USERNAME = "root";
    private final String PASSWORD = "";
    private final String LOCK_TABLE = "unit_test_lock_table";
    
    /**
     * Tests establishing a connection to 127.0.0.1 using default settings.
     * 
     * @since 1.0
     */
    @Test
    public void testConnection()
    {
        Postgres p = new Postgres(HOST, DB, USERNAME, PASSWORD, PORT);
        
        try
        {
            p.connect();
            assertTrue(true);
        }
        catch(DatabaseException ex)
        {
            fail("Postgres failed to connect - " + ex.getMessage());
        }
        
        p.disconnect();
    }
    /**
     * Tests table locking.
     * 
     * @since 1.0
     */
    @Test
    public void testLocking()
    {
        try
        {
            Postgres p = new Postgres(HOST, DB, USERNAME, PASSWORD, PORT);
            p.connect();
            
            p.execute("CREATE TABLE "+LOCK_TABLE+"();");
            
            p.tableLock(LOCK_TABLE, false);
            p.tableUnlock(false);
            
            p.execute("DROP TABLE "+LOCK_TABLE+";");
            assertTrue(true);
        }
        catch(DatabaseException ex)
        {
            fail("Failed Postgres lock test - "+ex.getMessage());
        }
    }
    /**
     * Tests accessors.
     * 
     * @since 1.0
     */
    @Test
    public void testAccessors()
    {
        Postgres p = new Postgres(HOST, DB, USERNAME, PASSWORD, PORT);
        
        assertTrue(p.getConnectorType() != 0);
    }
}
