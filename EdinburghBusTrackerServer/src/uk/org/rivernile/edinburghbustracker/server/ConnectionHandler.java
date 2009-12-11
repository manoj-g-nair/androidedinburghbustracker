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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * The ConnectionHandler class deals with the individual client connections to
 * the Bus Tracker Server and the requests that each of these clients may have.
 *
 * @author Niall Scott
 */
public class ConnectionHandler implements Runnable {

    private final static String httpAgent = "BusTrackerServer/" +
            Main.getVersion();
    private Socket clientSocket, serverSocket;
    private IncomingSocketHandler socketHandler;
    private BufferedReader clientIn, serverIn;
    private PrintWriter clientOut, serverOut;

    /**
     * Create a new ConnectionHandler.
     *
     * @param clientSocket The clientSocket of this particular client.
     * @param socketHandler The call back to the IncomingSocketHandler instance
     * so we can remove this object from the connection list once this task is
     * finished.
     */
    public ConnectionHandler(final Socket socket,
            final IncomingSocketHandler socketHandler) {
        if(socket == null) throw new IllegalArgumentException("The socket " +
                "instance must not be null.");
        if(socketHandler == null) throw new IllegalArgumentException("The " +
                "socket handler instance must not be null.");
        this.clientSocket = socket;
        this.socketHandler = socketHandler;
        try {
            clientIn = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));
            clientOut = new PrintWriter(socket.getOutputStream(), true);
        } catch(IOException e) {
            System.err.println("Exception while setting up socket " +
                    "input/output streams:");
            System.err.println(e.toString());
            System.err.println("Client IP address: " +
                    socket.getInetAddress().getHostAddress());
        }
    }

    /**
     * This is the entry point when the new clientSocket thread is created for
     * this class.
     */
    @Override
    public void run() {
        String line;
        String[] splitted;
        try {
            while((line = clientIn.readLine()) != null) {
                line = line.trim();
                if(line.length() == 0) continue;
                splitted = line.split(":");
                if(splitted[0].equals("getBusTimesByStopCode")) {
                    if(splitted.length != 2) {
                        clientOut.println("Error: the number of parameters " +
                                "for getBusTimesByStopCode is 1.");
                    } else {
                        getBusTimesByStopCode(splitted[1]);
                    }
                } else if(splitted[0].equals("exit")) {
                    clientSocket.close();
                    break;
                } else {
                    clientOut.println("Error: unknown server command.");
                }
            }
            clientIn.close();
            clientOut.close();
            clientSocket.close();
        } catch(IOException e) {
            // The clientSocket has probably been closed.
        }
        socketHandler.removeConnection(this);
    }

    /**
     * This is the handler for when the getBusTimesByStopCode:stopCode message
     * is received by the server. It contacts the Bus Tracker WAP website to
     * retrieve the data for that stop and transforms that data in to JSON
     * representation and replies to the client.
     *
     * @param stopCode The stop code argument supplied in the call from the
     * client.
     */
    public void getBusTimesByStopCode(final String stopCode) {
        if(stopCode == null) throw new IllegalArgumentException("The stop " +
                "code must not be null.");
        if(stopCode.length() == 0) throw new IllegalArgumentException("The " +
                "length of the stop code must not be 0.");
        try {
            serverSocket = new Socket(Config.getConfig().getMobileSiteURL(),
                    80);
            serverIn = new BufferedReader(new InputStreamReader(
                    serverSocket.getInputStream()));
            serverOut = new PrintWriter(serverSocket.getOutputStream(), true);
            String postString = "busStopCodeQuick=" + stopCode +
                    "&Navig=ResultStop1";
            //postString = URLEncoder.encode(postString.trim(), "UTF-8");
            serverOut.printf("POST /wap.php HTTP/1.1\r\n");
            serverOut.printf("Host: %s\r\n", Config.getConfig()
                    .getMobileSiteURL());
            serverOut.printf("User-Agent: %s\r\n", httpAgent);
            serverOut.printf("Content-Type: application/x-www-form-urlencoded" +
                    "\r\n");
            serverOut.printf("Content-Length: %d\r\n\r\n", postString.length());
            serverOut.printf("%s\r\n", postString);
            String line;
            while((line = serverIn.readLine()) != null) {
                if(line.startsWith("Incorrect input")) {
                    clientOut.println("Error: invalid bus stop code.");
                }
                if(line.trim().length() != 0) {
                    System.out.println(line);
                }
            }
            serverIn.close();
            serverOut.close();
            serverSocket.close();
            serverIn = null;
            serverOut = null;
            serverSocket = null;
        } catch(UnknownHostException e) {
            System.err.println("The system was unable to resolve the " +
                    "hostname of the mobile bus tracker website.");
        } catch(IOException e) {
            System.err.println("The socket to the mobile website was closed" +
                    "unexpectedly.");
        }
    }
}