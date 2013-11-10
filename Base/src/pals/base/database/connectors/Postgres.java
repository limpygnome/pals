package pals.base.database.connectors;

import java.sql.DriverManager;
import java.sql.SQLException;
import pals.base.database.*;

/**
 * The Postgres (PostgreSQL) database connector.
 */
public class Postgres extends Connector
{
    // Fields - Settings *******************************************************
    private final String    settingsHost,
                            settingsDatabase,
                            settingsUsername,
                            settingsPassword;
    private  final int      settingsPort;
    // Methods - Constructors **************************************************
    public Postgres(String settingsHost, String settingsDatabase, String settingsUsername, String settingsPassword, int settingsPort)
    {
        this.connection = null;
        this.settingsHost = settingsHost;
        this.settingsDatabase = settingsDatabase;
        this.settingsUsername = settingsUsername;
        this.settingsPassword = settingsPassword;
        this.settingsPort = settingsPort;
    }
    // Methods - Overrides *****************************************************
    @Override
    public void connect() throws DatabaseException
    {
        if(this.connection != null)
            throw new DatabaseException(DatabaseException.Type.ConnectionAlreadyEstablished);
        try
        {
            // Load the Postgres library into the runtime by fetching the class for the driver
            Class.forName("org.postgresql.Driver");
            // Create a new connection
            this.connection = DriverManager.getConnection("jdbc:postgresql://" + settingsHost + ":" + settingsPort + "/" + settingsDatabase, settingsUsername, settingsPassword);
        }
        catch(ClassNotFoundException | SQLException ex)
        {
            throw new DatabaseException(DatabaseException.Type.ConnectionFailure, ex);
        }
    }
    @Override
    public int getConnectorType()
    {
        return 90326058;
    }
}
