package pals.base.web;

import java.io.Serializable;
import java.nio.charset.Charset;

/**
 * A wrapper used to represent the response data for a web-request, which can be
 * transported over RMI.
 * 
 * Notes:
 * - MIME - Multipurpose Internet Mail Extensions; refer to RFC 1521:
 *   -- http://tools.ietf.org/html/rfc1521
 */
public class RemoteResponse implements Serializable
{
    // Fields - Constants ******************************************************
    /**
     * The default MIME response type.
     */
    public static final String DEFAULT_MIME_TYPE = "text/html;charset=UTF-8";
    // Fields ******************************************************************
    private String  sessionID;      // Session identifier.
    private byte[]  buffer;         // Response data to be written to the user.
    private String  responseType;   // The MIME response type of the data.
    // Methods - Constructors **************************************************
    public RemoteResponse()
    {
        this.sessionID = null;
        this.buffer = null;
        this.responseType = DEFAULT_MIME_TYPE;
    }
    // Methods - Mutators ******************************************************
    /**
     * @param data The data to be placed in the buffer, for writing to the user.
     */
    public void setBuffer(byte[] data)
    {
        this.buffer = data;
    }
    /**
     * @param data The string data to be transformed from a UTF-8 string into
     * bytes, and then placed in a buffer for writing to the user.
     */
    public void setBuffer(String data)
    {
        this.buffer = data.getBytes(Charset.forName("UTF-8"));
    }
    /**
     * @param sessionID Sets the session ID to deliver to the user, for cookie
     * management; can be null.
     */
    public void setSessionID(String sessionID)
    {
        this.sessionID = sessionID;
    }
    /**
     * @param responseType The MIME type of the response data; cannot be null.
     */
    public void setResponseType(String responseType) throws IllegalArgumentException
    {
        if(responseType == null)
            throw new IllegalArgumentException("MIME response type cannot be null!");
        this.responseType = responseType;
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The buffer of data to write to the end-user.
     */
    public byte[] getBuffer()
    {
        return buffer;
    }
    /**
     * @return Retrieves the session ID associated with the current user; can
     * be null.
     */
    public String getSessionID()
    {
        return sessionID;
    }
    /**
     * @return The MIME type of the response data.
     */
    public String getResponseType()
    {
        return responseType;
    }
}
