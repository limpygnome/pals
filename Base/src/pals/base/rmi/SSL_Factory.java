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
package pals.base.rmi;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * A factory for creating SSL server and client sockets, using a custom
 * keystore. Avoids having to apply a global keystore and/or trustmanager
 * to the entire application, which may conflict with other plugins.
 */
public class SSL_Factory implements RMIClientSocketFactory, RMIServerSocketFactory, Serializable
{
    // Fields ******************************************************************
    private SSLSocketFactory        cfact;      // Creates client SSL sockets.
    private SSLServerSocketFactory  sfact;      // Creates server SSL sockets.
    private SSLContext              context;    // The current SSL context - points to the keystore and trustmanager.
    // Methods - Constructors **************************************************
    public SSL_Factory(String keystorePath, String keystorePassword)
    {
        try
        {
            char[] password = keystorePassword.toCharArray();
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(new FileInputStream(keystorePath), password);
            
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, password);
            
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ks);
            
            context = SSLContext.getInstance("TLS");

            context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            
            cfact = context.getSocketFactory();
            sfact = context.getServerSocketFactory();
        }
        catch(Exception ex)
        {
            System.err.println("ERROR: "+ex.getMessage());
        }
    }
    /**
     * Creates a new client SSL socket.
     * 
     * @param host The destination hostname/IP.
     * @param port The destination port.
     * @return An instance of a client socket.
     * @throws IOException Thrown if a socket cannot be made.
     */
    @Override
    public Socket createSocket(String host, int port) throws IOException
    {
        return cfact.createSocket(host, port);
    }
    /**
     * Creates a new server SSL socket for listening.
     * 
     * @param port The port for listening.
     * @return An instance of a server socket.
     * @throws IOException Thrown if a socket cannot be made.
     */
    @Override
    public ServerSocket createServerSocket(int port) throws IOException
    {
        return sfact.createServerSocket(port);
    }
}