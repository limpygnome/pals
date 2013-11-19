package pals.base.web;

import java.io.Serializable;

/**
 * A wrapper used to represent the response data for a web-request, which can be
 * transported over RMI.
 */
public class RemoteResponse implements Serializable
{
    private byte[] buffer;
}
