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

import java.sql.DriverManager;
import java.sql.SQLException;
import pals.base.database.*;

/**
 * The MySQL database connector.
 * 
 * NOTE: this is currently not completely supported, but has been left for
 * future support.
 * 
 * @version 1.0
 */
public class MySQL extends Connector
{
    // Constants ***************************************************************
    /**
     * The unique identifier of this connector.
     * 
     * @since 1.0
     */
    public static final int IDENTIFIER_TYPE = 1453673645;
    // Fields - Settings *******************************************************
    private final String    settingsHost,
                            settingsDatabase,
                            settingsUsername,
                            settingsPassword;
    private  final int      settingsPort;
    // Methods - Constructors **************************************************
    /**
     * Constructs a new instance.
     * 
     * @param settingsHost Host IP/name.
     * @param settingsDatabase Database.
     * @param settingsUsername Username.
     * @param settingsPassword Password.
     * @param settingsPort Port of host.
     * @since 1.0
     */
    public MySQL(String settingsHost, String settingsDatabase, String settingsUsername, String settingsPassword, int settingsPort)
    {
        this.connection = null;
        this.settingsHost = settingsHost;
        this.settingsDatabase = settingsDatabase;
        this.settingsUsername = settingsUsername;
        this.settingsPassword = settingsPassword;
        this.settingsPort = settingsPort;
    }
    // Methods - Overrides *****************************************************
    /**
     * Connects to the host.
     * 
     * @throws DatabaseException Thrown if a connection cannot be established.
     * @since 1.0
     */
    @Override
    public void connect() throws DatabaseException
    {
        if(this.connection != null)
            throw new DatabaseException(DatabaseException.Type.ConnectionAlreadyEstablished);
        try
        {
            // Load the Postgres library into the runtime by fetching the class for the driver
            Class.forName("com.mysql.jdbc.Driver");
            // Create a new connection
            this.connection = DriverManager.getConnection("jdbc:mysql://" + settingsHost + ":" + settingsPort + "/" + settingsDatabase, settingsUsername, settingsPassword);
        }
        catch(ClassNotFoundException | SQLException ex)
        {
            throw new DatabaseException(DatabaseException.Type.ConnectionFailure, ex);
        }
    }
    /**
     * @see Connector#getConnectorType()
     * 
     * @return The unique identifier.
     * @since 1.0
     */
    @Override
    public int getConnectorType()
    {
        return IDENTIFIER_TYPE;
    }
}
