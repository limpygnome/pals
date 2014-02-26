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
    Version:    1.0
    Authors:    Marcus Craske           <limpygnome@gmail.com>
    ----------------------------------------------------------------------------
*/
package pals.rmi;

import pals.base.UUID;

/**
 * A model for holding RMI information for a node.
 */
public class RMI_Host
{
    // Fields ******************************************************************
    private UUID    uuid;
    private String  host;
    private int     port;
    // Methods - Constructors **************************************************
    public RMI_Host(UUID uuid, String host, int port)
    {
        this.uuid = uuid;
        this.host = host;
        this.port = port;
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The UUID of the node.
     */
    public UUID getUUID()
    {
        return uuid;
    }
    /**
     * @return The IP of the host.
     */
    public String getHost()
    {
        return host;
    }
    /**
     * @return The port of the RMI registry of the host.
     */
    public int getPort()
    {
        return port;
    }
}
