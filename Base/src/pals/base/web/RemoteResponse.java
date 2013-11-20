package pals.base.web;

import java.io.Serializable;
import java.nio.charset.Charset;

/**
 * A wrapper used to represent the response data for a web-request, which can be
 * transported over RMI.
 */
public class RemoteResponse implements Serializable
{
    private byte[] buffer;
    
    public RemoteResponse()
    {
        this.buffer = null;
    }
    
    public void setBuffer(byte[] data)
    {
        this.buffer = data;
    }
    public void setBuffer(String data)
    {
        this.buffer = data.getBytes(Charset.forName("UTF-8"));
    }
    
    public byte[] getBuffer()
    {
        return buffer;
    }
}
