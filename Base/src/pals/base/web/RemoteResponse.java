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
package pals.base.web;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.HashMap;

/**
 * A wrapper used to represent the response data for a web-request, which can be
 * transported over RMI.
 * 
 * Notes:
 * - MIME - Multipurpose Internet Mail Extensions; refer to RFC 1521:
 *   -- http://tools.ietf.org/html/rfc1521
 * - Session private should be used to timeout the session, when idle; refer to
 * the following for guidelines:
 * https://www.owasp.org/index.php/Session_Management#Ensure_Idle.2C_absolute_timeouts_are_as_short_as_practical
 * 
 * @version 1.0
 */
public class RemoteResponse implements Serializable
{
    static final long serialVersionUID = 1L;
    // Fields - Constants ******************************************************
    /**
     * The default MIME response type.
     * 
     * @since 1.0
     */
    public static final String DEFAULT_MIME_TYPE = "text/html;charset=UTF-8";
    // Fields ******************************************************************
    private String                  sessionID;      // Session identifier.
    private boolean                 sessionPrivate; // Indicates if the user session is private, and thus should expire after a longer duration.
    private byte[]                  buffer;         // Response data to be written to the user.
    private String                  responseType;   // The MIME response type of the data.
    private String                  urlRedirect;    // Used for redirecting to a new URL.
    private int                     responseCode;   // The response status/code.
    private HashMap<String,String>  headers;        // Any header data to be appended.
    // Methods - Constructors **************************************************
    /**
     * Constructs a new instance.
     * 
     * @since 1.0
     */
    public RemoteResponse()
    {
        this.sessionID = null;
        this.sessionPrivate = false;
        this.buffer = null;
        this.responseType = DEFAULT_MIME_TYPE;
        this.urlRedirect = null;
        this.responseCode = 200;
        this.headers = null;
    }
    // Methods - Mutators ******************************************************
    /**
     * Sets the response buffer.
     * 
     * @param data The data to be placed in the buffer, for writing to the
     * user.
     * @since 1.0
     */
    public void setBuffer(byte[] data)
    {
        this.buffer = data;
    }
    /**
     * Sets the buffer.
     * 
     * @param data The string data to be transformed from a UTF-8 string into
     * bytes, and then placed in a buffer for writing to the user.
     * @since 1.0
     */
    public void setBuffer(String data)
    {
        this.buffer = data == null ? null : data.getBytes(Charset.forName("UTF-8"));
    }
    /**
     * Sets the session ID.
     * 
     * @param sessionID Sets the session ID to deliver to the user, for cookie
     * management; can be null.
     * @since 1.0
     */
    public void setSessionID(String sessionID)
    {
        this.sessionID = sessionID;
    }
    /**
     * Sets if the session is private.
     * 
     * @param sessionPrivate Sets if the user session is private, and thus
     * should expire after a longer duration.
     * @since 1.0
     */
    public void setSessionPrivate(boolean sessionPrivate)
    {
        this.sessionPrivate = sessionPrivate;
    }
    /**
     * Sets the response-type.
     * 
     * @param responseType The MIME type of the response data; cannot be null.
     * @since 1.0
     */
    public void setResponseType(String responseType) throws IllegalArgumentException
    {
        if(responseType == null)
            throw new IllegalArgumentException("MIME response type cannot be null!");
        this.responseType = responseType;
    }
    /**
     * Sets the redirect URL.
     * 
     * @param url The URL of where to redirect the response.
     * @since 1.0
     */
    public void setRedirectUrl(String url)
    {
        urlRedirect = url;
    }
    /**
     * Sets the response-code.
     * 
     * @param responseCode The new response code.
     * @since 1.0
     */
    public void setResponseCode(int responseCode)
    {
        this.responseCode = responseCode;
    }
    /**
     * Sets a HTTP header.
     * 
     * @param k The key of the header.
     * @param v The value of the header.
     * @since 1.0
     */
    public void setHeader(String k, String v)
    {
        if(headers == null)
            headers = new HashMap<>();
        headers.put(k, v);
    }
    /**
     * Removes a header from the HTTP headers collection.
     * 
     * @param k The key of the header to remove.
     * @since 1.0
     */
    public void removeHeader(String k)
    {
        if(headers != null)
        {
            headers.remove(k);
            if(headers.isEmpty())
                headers = null;
        }
    }
    // Methods - Accessors *****************************************************
    /**
     * Retrieves the response buffer.
     * 
     * @return The buffer of data to write to the end-user.
     * @since 1.0
     */
    public byte[] getBuffer()
    {
        return buffer;
    }
    /**
     * Retrieves the session ID.
     * 
     * @return Retrieves the session ID associated with the current user; can
     * be null.
     * @since 1.0
     */
    public String getSessionID()
    {
        return sessionID;
    }
    /**
     * Indicates if the user session is private, and thus should expire
     * after a longer duration.
     * 
     * @return True = private, false = not private.
     * @since 1.0
     */
    public boolean isSessionPrivate()
    {
        return sessionPrivate;
    }
    /**
     * Retrieves the response-type.
     * 
     * @return The MIME type of the response data.
     * @since 1.0
     */
    public String getResponseType()
    {
        return responseType;
    }
    /**
     * Gets the redirect URL.
     * 
     * @return Redirect URL.
     * @since 1.0
     */
    public String getRedirectUrl()
    {
        return urlRedirect;
    }
    /**
     * Gets the response-code for the current request.
     * 
     * @return Response code.
     * @since 1.0
     */
    public int getResponseCode()
    {
        return responseCode;
    }
    /**
     * Gets the HTTP headers to be set for the response; this should not be
     * used to set data.
     * 
     * @return Internal data-structure.
     * @since 1.0
     */
    public HashMap<String,String> getHeaders()
    {
        return headers == null ? new HashMap<String, String>() : headers;
    }
    /**
     * Indicates if header-data is available.
     * 
     * @return True = available, false = not available.
     * @since 1.0
     */
    public boolean isHeadersAvailable()
    {
        return headers != null && !headers.isEmpty();
    }
}
