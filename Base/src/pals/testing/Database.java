
package pals.testing;

import pals.base.UUID;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.database.Result;
import pals.base.database.connectors.MySQL;
import pals.base.database.connectors.Postgres;

public class Database
{
    public static void main(String[] args)
    {
        boolean failed = false;
        Connector conn;
        // Postgres
        try
        {
            conn = new Postgres("127.0.0.1", "test", "root", "", 5432);
            conn.connect();
            System.out.println("Testing Postgres...");
            dbTest(conn);
            conn.disconnect();
        }
        catch(DatabaseException ex)
        {
            System.err.println("Postgres failure: '" + ex.getType().toString() + "' ~ " + (ex.getCause() != null ? ex.getCause().getMessage() : "[no root cause]"));
            failed = true;
        }
        // MySQL
        try
        {
            conn = new MySQL("127.0.0.1", "test", "root", "", 3306);
            conn.connect();
            System.out.println("Testing MySQL...");
            dbTest(conn);
            conn.disconnect();
        }
        catch(DatabaseException ex)
        {
            System.err.println("MySQL failure: '" + ex.getType().toString() + "' ~ " + (ex.getCause() != null ? ex.getCause().getMessage() : "[no root cause]"));
            failed = true;
        }
        if(failed)
            System.err.println("Failed automated tests!");
        else
            System.out.println("Success!");
    }
    public static void dbTest(Connector conn) throws DatabaseException
    {
        System.out.println("- Creating table and inserting test record...");
        conn.execute("CREATE TABLE test ( a INT );");
        conn.execute("INSERT INTO test (a) VALUES('1234');");
        System.out.println("- Created table and inserted record.");
        
        System.out.println("- Reading data from table...");
        Result result = conn.read("SELECT * FROM test;");
        int row = 0;
        int value;
        while(result.next())
        {
            value = result.get("a");
            System.out.println("[Row " + row++ + "] Value: '" + value + "'");
        }
        result.dispose();
        System.out.println("- Finished read.");
        
        System.out.println("- Dropping  table...");
        conn.execute("DROP TABLE test;");
        System.out.println("- Dropped table.");
        
        System.out.println("- Testing UUID; creating table...");
        switch(conn.getConnectorType())
        {
            case Postgres.IDENTIFIER_TYPE:
                conn.execute("CREATE TABLE test ( uuid BYTEA );"); break;
            case MySQL.IDENTIFIER_TYPE:
                conn.execute("CREATE TABLE test ( uuid BINARY(16) );"); break;
        }
        
        System.out.println("- Inserting UUID data...");
        int uuids = 4;
        UUID[] uuidIndex = new UUID[uuids];
        for(int i = 0; i < uuids; i++)
        {
            uuidIndex[i] = UUID.generateVersion4();
            conn.execute("INSERT INTO test (uuid) VALUES(?);", uuidIndex[i].getBytes());
        }
        
        System.out.println("- Reaading back UUID data and checking...");
        result = conn.read("SELECT * FROM test;");
        byte[] data;
        UUID uuid;
        int i = 0;
        while(result.next())
        {
            data = result.get("uuid");
            if((uuid = UUID.parse(data)) == null)
                System.err.println("-- Failed to parse UUID!");
            else
            {
                System.out.println("-- Parsed UUID ~ " + uuid.getHexHyphens());
                if(uuid.getHex().equals(uuidIndex[i].getHex()))
                    System.out.println("-- Data is correct...");
                else
                    System.err.println("-- Incorrect UUID data read ~ '" + uuid.getHex() + "' vs original '" + uuidIndex[i].getHex() + "'!");
            }
            i++;
        }
        result.dispose();
        
        System.out.println("- Dropping  table...");
        conn.execute("DROP TABLE test;");
        
        System.out.println("- End of test!");
    }
}
