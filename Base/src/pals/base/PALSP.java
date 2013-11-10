package pals.base;

import java.net.ServerSocket;
import java.net.Socket;
import pals.base.database.Connector;

/**
 * PALS protocol: responsible for handling data-exchange between nodes; this is
 * for transferring files.
 * 
 * About
 * *****************************************************************************
 * All nodes are a client, however there can be master nodes. These master nodes
 * are used by client nodes, who are not masters, to sync plugin files. Nodes
 * can also e.g. transfer files, which may be used for assessing work, and carry
 * out other operations - this is up to plugins to register hooks with the
 * system for handling inbound data. Any plugin can also send outbound data.
 * 
 * Nodes can also use SSL certificates for authentication.
 * 
 * Nodes also both receive and send messages.
 * 
 * Protocol
 * *****************************************************************************
 * Each message starts with the UUID of the node sending the message, which
 * consists of eight bytes. Since nodes can use SSL certificates, it is assumed
 * the network cannot be penetrated and disrupted with fake identifiers. These
 * identifiers should also not be used as any type of security validation.
 * 
 * Next the message consists of a command, which is a UUID / 16 bytes. This
 * is used with a look-up table to determine which part of the system is
 * responsible for handling the message.
 * 
 * Next, the length of the message is sent as eight bytes (a 64-bit unsigned
 * number, can be zero). The receiver will then read the specified number of
 * bytes; these bytes are assumed to be data. The maximum length of a message
 * is controlled by PALSP_DATA_LIMIT.
 * 
 * The receiver of the message then sends a byte to indicate the status of
 * handling the message, refer to Message class.
 * 
 * This is the end of the procedure for sending a message. The connection can
 * remain open and a new message sent, however the connection may be closed by
 * either end.
 */
public class PALSP
{
    // Constants ***************************************************************
    /**
     * The maximum length of a PALSP data message (in bytes).
     */
    public final int PALSP_DATA_LIMIT = 26214400;       // 25 megabytes
    // Fields ******************************************************************
    private Socket          socket;         // The socket responsible for sending data.
    private ServerSocket    socketServer;   // The socket responsible for receiving data.
    // Methods - Constructors **************************************************
    protected PALSP()
    {
        this.socket = null;
        this.socketServer = null;
    }
    // Methods - General
    /**
     * Reloads a map of node UUIDs and their IP addresses.
     * @param connector
     * @return 
     */
    public boolean reloadAddressBook(Connector connector)
    {
        return false;
    }
    // Methods - Server
    public boolean clientConnect(UUID serverUUID)
    {
        return false;
    }
    public void clientDisconnect()
    {
    }
    public boolean clientSend()
    {
        return false;
    }
    // add ability to make the socket an SSL socket
    
    // pals protocol
    
    // -- methods here for sending data over a socket between other nodes etc
    
}
