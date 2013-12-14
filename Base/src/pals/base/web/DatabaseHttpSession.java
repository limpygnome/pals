package pals.base.web;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import pals.base.database.Connector;
import org.apache.commons.codec.binary.Base64;
import pals.base.database.DatabaseException;
import pals.base.database.Result;
import pals.base.utils.DateTime;

/**
 * An alternate HttpSession manager, which persists a web-user's session to a
 * database; allows multiple machines to access session data.
 * 
 * Session identifiers are random bytes, with the length specified by
 * KEY_LENGTH_MAX. Sessions are also binded to IP addresses to further protect
 * against brute-forcing of the session identifier. Theoretically it is possible
 * for a user at the same IP address to receive the same session ID and thus
 * access to another user's session. However this is generally accepted as
 * secure, providing the entropy of the ID is significantly large.
 * 
 * The random number generated is only created once, with the following as the
 * seed:
 * (Math.random()*(1-Math.random()*)100) + (System.currentTimeMillis() * Math.Random())
 * + ID_SIZE + KEY_LENGTH_MAX
 * 
 * This is then used with a new random number generator for each identifier
 * created, which uses a value from the previous generator and
 * System.currentTimeMillis() as a seed. This helps to protect against guessing
 * the seed value, which otherwise could allow the entire sequence of
 * pseudo-random numbers to be guessed.
 * 
 * The guidelines provided by OWASP have been followed:
 * https://www.owasp.org/index.php/Session_Management
 * 
 * Thread-safe collection.
 */
public class DatabaseHttpSession
{
    // Enums *******************************************************************
    private enum SValueChange
    {
        Added,
        Modified,
        Removed,
    }
    // Constants ***************************************************************
    private static final Random rng;                    // RNG for session IDs - made final to protect seed value; else a generated ID could be guessed (theoretically).
    private static final int    ID_SIZE = 512;          // The maximum length (bytes) of a session identifier.
    private static final int    KEY_LENGTH_MAX = 32;    // The maximum length of a key.
    static
    {
        // Create new RNG using a very unique seed
        rng = new Random(
                (int)( (Math.random()*(1-Math.random()))+( (double)System.currentTimeMillis() * Math.random()))
                + ID_SIZE + KEY_LENGTH_MAX
        );
    }
    // Fields ******************************************************************
    private byte[]                              sessid;     // The session identifier.
    private final HashMap<String,Object>        data;       // A copy of the session data retrieved from the server.
    private final HashMap<String,SValueChange>  changes;    // Tracks the type of changes (value) of attributes (key).
    private final String                        ipAddress;  // The IP address of the session; further protects against brute-forcing of the session ID.
    // Methods - Constructors **************************************************
    private DatabaseHttpSession(String ipAddress)
    {
        this.sessid = null;
        this.data = new HashMap<>();
        this.changes = new HashMap<>();
        this.ipAddress = ipAddress;
    }
    // Methods - Static ********************************************************
    /**
     * Loads an instance of a HTTP session; this may be from a previous or new
     * session.
     * 
     * @param conn Database connector.
     * @param base64id Base64 string of session identifier; can be null.
     * @param ipAddress IP address of client; cannot be null or empty.
     * @return Instance of this class for a user.
     * 
     * @throws IllegalArgumentException Thrown if an IP address is invalid.
     * @throws DatabaseException Thrown if an error occurs communicating to the
     * database.
     * @throws IOException Thrown if an object cannot be deserialized.
     * @throws ClassNotFoundException Thrown if an object cannot be deserialized
     * because its original class is missing.
     */
    public static DatabaseHttpSession load(Connector conn, String base64id, String ipAddress) throws IllegalArgumentException, DatabaseException, IOException, ClassNotFoundException
    {
        // Check the IP address is valid
        if(ipAddress == null || ipAddress.length() == 0)
            throw new IllegalArgumentException("Attempted to create a DatabaseHttpSession with an invalid IP address!");
        // Create instance of the session object
        DatabaseHttpSession session = new DatabaseHttpSession(ipAddress);
        // Parse session ID (else generate a new one)
        if(base64id != null)
        {
            session.sessid = Base64.decodeBase64(base64id);
            // Check the session ID is of a valid length
            if(session.sessid.length != ID_SIZE)
                session.sessid = null;
        }
        // Validate session ID
        if(session.sessid != null)
        {
            // Check the session belongs to the IP
            Result sess = conn.read("SELECT ip FROM pals_http_sessions WHERE ip=? AND sessid =?;", ipAddress, session.sessid);
            if(!sess.next() || !ipAddress.equals(sess.get("ip")))
                session.sessid = null;
            else
            {
                // Load a snapshot of the session data
                Result sessionData = conn.read("SELECT key, data FROM pals_http_session_data WHERE sessid=?;", session.sessid);
                ByteArrayInputStream deserialBais;
                ObjectInputStream deserialOis;
                while(sessionData.next())
                {
                    // Deserialize data
                    deserialBais = new ByteArrayInputStream((byte[])sessionData.get("data"));
                    deserialOis = new ObjectInputStream(deserialBais);
                    // Add to map
                    session.data.put((String)sessionData.get("key"), deserialOis.readObject());
                }
            }
        }
        else
        {
            // Generate a new ID
            session.sessid = generateId();
        }
        return session;
    }
    // Methods *****************************************************************
    /**
     * Persists the session's data to the database; this should also be invoked
     * to update the user's last-active parameter (to avoid session timeout).
     * 
     * @param conn Database connector.
     * @throws DatabaseException Thrown if an issue occurs communicating with
     * the database.
     * @throws IOException Thrown if an issue occurs serializing an object to
     * the database.
     */
    public synchronized void persist(Connector conn) throws DatabaseException, IOException
    {
        // Check there is session data/changes - else there is no need to do anything on the database!
        if(data.isEmpty() && changes.isEmpty())
            return;
        // Begin SQL transaction
        conn.execute("BEGIN;");
        // Check the session still exists
        // Note: count is bigint / long: http://www.postgresql.org/docs/8.2/static/functions-aggregate.html
        if((long)conn.executeScalar("SELECT COUNT(sessid) FROM pals_http_sessions WHERE sessid=? AND ip=?;", sessid, ipAddress) == 0)
        {
            // Create session record
            conn.execute("INSERT INTO pals_http_sessions (sessid, creation, last_active, ip) VALUES(?,current_timestamp,current_timestamp,?);", sessid, ipAddress);
        }
        else
        {
            // Update the time of last-active to now
            conn.execute("UPDATE pals_http_sessions SET last_active=current_timestamp WHERE sessid=?;", sessid);
        }
        // Persist changed session data
        Object k;
        ByteArrayOutputStream serializeBaos;
        ObjectOutput serializeOo;
        byte[] serializeData;
        for(Map.Entry<String,SValueChange> change : changes.entrySet())
        {
            k = change.getKey();
            // Persist change to the database...
            switch(change.getValue())
            {
                case Added:
                case Modified:
                    // Serialize the data to a byte-array
                    {
                        serializeBaos = new ByteArrayOutputStream();
                        serializeOo = new ObjectOutputStream(serializeBaos);
                        serializeOo.writeObject(data.get(k));
                        serializeOo.flush();
                        serializeData = serializeBaos.toByteArray();
                    }
                    // Delete the existing record and add a new one
                    // -- A session could have added a new record
                    conn.execute("DELETE FROM pals_http_session_data WHERE sessid=? AND key=?; INSERT INTO pals_http_session_data (sessid, key, data) VALUES(?,?,?);", sessid, k, sessid, k, serializeData);
                    break;
                case Removed:
                    // Remove the key...
                    conn.execute("DELETE FROM pals_http_session_data WHERE sessid=? AND key=?;", sessid, change.getKey());
                    break;
            }
        }
        // Commit and clear the changes
        conn.execute("COMMIT;");
        changes.clear();
    }
    /**
     * Destroys the current session and generates a new identifier.
     * 
     * @param conn Database connector.
     * @throws DatabaseException Thrown if an issue occurs communicating with
     * the database.
     */
    public synchronized void destroy(Connector conn) throws DatabaseException
    {
        // Destroy old session data on the database
        conn.execute("DELETE FROM pals_http_sessions WHERE sessid=? AND ip=?;", sessid, ipAddress);
        // Generate new session identifier
        sessid = generateId();
    }
    private static byte[] generateId()
    {
        // Create new RNG
        Random ran = new Random(System.currentTimeMillis()+rng.nextInt());
        // Generate random bytes
        byte[] result = new byte[ID_SIZE];
        for(int i = 0; i < ID_SIZE; i++)
            result[i] = (byte)ran.nextInt(256); // n is exclusive; 0-255
        return result;
    }
    // Methods - Mutators ******************************************************
    /**
     * Sets a session attribute.
     * 
     * @param <T> The type of the value; must be Serializable.
     * @param key The key of the attribute.
     * @param value The value/instance of the attribute; can be null.
     */
    public synchronized <T extends Serializable> void setAttribute(String key, T value)
    {
        // Set the change to occur
        if(changes.containsKey(key))
        {
            switch(changes.get(key))
            {
                case Removed:
                    // The key must have existed, thus set to modified...
                    changes.put(key, SValueChange.Modified);
                    break;
            }
        }
        else if(data.containsKey(key))
        {
            // Key has been modified from its original state...
            changes.put(key, SValueChange.Modified);
        }
        else
        {
            // New key to the database...
            changes.put(key, SValueChange.Added);
        }
        // Update the field
        data.put(key, value);
    }
    /**
     * Removes an attribute from the session.
     * 
     * @param key The key of the attribute.
     */
    public synchronized void removeAttribute(String key)
    {
        if(changes.containsKey(key))
        {
            switch(changes.get(key))
            {
                case Added:
                    // No change has occurred on the database side...
                    changes.remove(key);
                    break;
                default:
                    // The key existed - thus it needs removing...
                    changes.put(key, SValueChange.Removed);
            }
        }
        else
            changes.put(key, SValueChange.Removed);
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The number of session attributes.
     */
    public int size()
    {
        return data.size();
    }
    /**
     * @return The session ID.
     */
    public byte[] getId()
    {
        return sessid;
    }
    /**
     * @return The session ID, as a base-64 string.
     */
    public String getIdBase64()
    {
        return Base64.encodeBase64String(sessid);
    }
    /**
     * Retrieves a session attribute.
     * 
     * @param <T> The type of the value; must be Serializable.
     * @param key The key of the attribute.
     * @return The value/instance of the attribute; can be null.
     */
    public <T extends Serializable> T getAttribute(String key)
    {
        return (T)data.get(key);
    }
    /**
     * @return The IP address of the current session.
     */
    public String getIpAddress()
    {
        return ipAddress;
    }
}
