/*
 * Copyright (C) 2009 Niall 'Rivernile' Scott
 *
 * This software is provided 'as-is', without any express or implied
 * warranty.  In no event will the authors or contributors be held liable for
 * any damages arising from the use of this software.
 *
 * The aforementioned copyright holder(s) hereby grant you a
 * non-transferrable right to use this software for any purpose (including
 * commercial applications), and to modify it and redistribute it, subject to
 * the following conditions:
 *
 *  1. This notice may not be removed or altered from any file it appears in.
 *
 *  2. Any modifications made to this software, except those defined in
 *     clause 3 of this agreement, must be released under this license, and
 *     the source code of any modifications must be made available on a
 *     publically accessible (and locateable) website, or sent to the
 *     original author of this software.
 *
 *  3. Software modifications that do not alter the functionality of the
 *     software but are simply adaptations to a specific environment are
 *     exempt from clause 2.
 */

package uk.org.rivernile.edinburghbustracker.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

/**
 * The IncomingSocketHandler class deals with the server socket connections
 * abstractly and then passes the new socket connection to a new thread in the
 * ConnectionHandler class.
 *
 * @author Niall Scott
 */
public class IncomingSocketHandler {

    private ServerSocket listenSocket;
    private final LinkedList<ConnectionHandler> connectionList;
    private final Object listLock;
    private boolean keepRunning;

    /**
     * Creates a new IncomingSocketHandler. This constructor does not accept any
     * paramters.
     */
    public IncomingSocketHandler() {
        connectionList = new LinkedList<ConnectionHandler>();
        listLock = new Object();
        keepRunning = true;
    }

    /**
     * This is the main loop of the whole server. It sets up the socket and then
     * proceeds to loop indefinitely, blocking while it waits for incoming
     * connections.
     */
    public void run() {
        try {
            listenSocket = new ServerSocket();
            listenSocket.setReuseAddress(true);
            listenSocket.setPerformancePreferences(1, 2, 0);
            listenSocket.bind(new InetSocketAddress(
                    Config.getConfig().getAddressToBind(),
                    Config.getConfig().getPortNumber()));
        } catch(IOException e) {
            System.err.println("Exception while creating listening socket: " +
                    e.toString());
            System.err.println("Server exiting.");
            System.exit(-1);
        }

        System.out.println("The socket is now listening.");
        Socket socket;
        PrintWriter writer;
        ConnectionHandler temp;
        int listSize;
        while(keepRunning) {
            try {
                socket = listenSocket.accept();
                socket.setSoTimeout(120000);
                synchronized(listLock) {
                    listSize = connectionList.size();
                }
                if(Config.getConfig().getMaxConnections() != 0 &&
                        listSize >= Config.getConfig().getMaxConnections()) {
                    writer = new PrintWriter(socket.getOutputStream(), true);
                    writer.println("Error: server has reached maximum number " +
                            "of connections.");
                    writer.close();
                    socket.close();
                } else {
                    temp = new ConnectionHandler(socket, this);
                    synchronized(listLock) {
                        connectionList.add(temp);
                    }
                    new Thread(temp).start();
                }
            } catch(IOException e) {
                System.err.println("Exception while accepting incoming " +
                        "connection: ");
                System.err.println(e.toString());
            }
        }
    }

    /**
     * Remove a connection from the connection list once it has finished dealing
     * with it's connection from the client. This method is thread safe.
     *
     * @param connection The connection to remove.
     */
    protected void removeConnection(
            final ConnectionHandler connection) {
        if(connection == null) throw new IllegalArgumentException("A non " +
                "null connection handler must be provided.");
        synchronized(listLock) {
            connectionList.remove(connection);
        }
    }

    /**
     * Stops the server by closing the socket then ending the socket listen
     * loop.
     */
    public void stopServer() {
        keepRunning = false;
        try {
            listenSocket.close();
        } catch(IOException e) {
            // Do nothing as server socket is closing anyway.
        }
    }
}