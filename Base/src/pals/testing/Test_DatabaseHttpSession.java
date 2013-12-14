
package pals.testing;

import java.io.IOException;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;
import pals.base.database.connectors.Postgres;
import pals.base.web.DatabaseHttpSession;

public class Test_DatabaseHttpSession
{
    public static void main(String[] args) throws DatabaseException, IllegalArgumentException, IOException, ClassNotFoundException
    {
        Connector conn = new Postgres("localhost", "pals", "root", "", 5432);
        try
        {
            conn.connect();
        }
        catch(DatabaseException ex)
        {
            System.err.println("Failed to connect to the database!");
        }
        // Create new session
        DatabaseHttpSession session = DatabaseHttpSession.load(conn, null, "127.0.0.1");
        session.setAttribute("var 1", 1);
        session.setAttribute("var 2", 2);
        session.setAttribute("var 3", 3);
        session.persist(conn);
        
        String sessid = session.getIdBase64();
        System.out.println("Session ID: '" + sessid + "'");
        
        // Re-open the session and check it correctly stored the data...
        session = DatabaseHttpSession.load(conn, sessid, "127.0.0.1");
        if((int)session.getAttribute("var 1") != 1)
            System.err.println("Variable 1 failed.");
        if((int)session.getAttribute("var 2") != 2)
            System.err.println("Variable 2 failed. ~ '" + session.getAttribute("var 2") + "'");
        if((int)session.getAttribute("var 3") != 3)
            System.err.println("Variable 3 failed.");
        // Remove variables 1 and 2 and persist...
        session.removeAttribute("var 1");
        session.setAttribute("var 2", 123);
        session.persist(conn);
        
        // Re-open the session and check it correctly removed var 1, changed var
        // 2 and kept var 3
        session = DatabaseHttpSession.load(conn, sessid, "127.0.0.1");
        if(session.getAttribute("var 1") != null)
            System.err.println("Did not remove var 1... ~ '" + session.getAttribute("var 1") + "'");
        if((int)session.getAttribute("var 2") != 123)
            System.err.println("Did not correctly update var 2...");
        if((int)session.getAttribute("var 3") != 3)
            System.err.println("Value of var 3 has unexpectedly changed...");
        
        // Destroy the session and check it has been destroyed...
        session.destroy(conn);
        if((session = DatabaseHttpSession.load(conn, sessid, "127.0.0.1")).size() != 0)
            System.out.println("Session was not destroyed (correctly?)...");
        
        System.out.println("End of test.");
    }
}
