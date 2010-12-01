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

import uk.org.rivernile.edinburghbustracker.server.livedata.LiveBusStopData;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import uk.org.rivernile.edinburghbustracker.server.stoplocations.Database;

/**
 * The ConnectionHandler class deals with the individual client connections to
 * the Bus Tracker Server and the requests that each of these clients may have.
 *
 * @author Niall Scott
 */
public class ConnectionHandler implements Runnable {

    private Socket clientSocket;
    private IncomingSocketHandler socketHandler;
    private BufferedReader clientIn;
    private PrintWriter clientOut;

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
                } else if(splitted[0].equals("getDBURL")) {
                    clientOut.println(Config.getConfig().getDBURL());
                } else if(splitted[0].equals("getDBLastModTime")) {
                    File f = new File(Database.LAST_MOD_FILE);
                    if(!f.exists() || f.length() == 0L) {
                        clientOut.println("0");
                        continue;
                    }
                    BufferedReader in = new BufferedReader(new FileReader(f));
                    clientOut.println(in.readLine());
                    in.close();
                } else if(splitted[0].equals("getLatestAndroidClientVersion")) {
                    File f = new File("latest.android");
                    if(!f.exists() || f.length() == 0L) {
                        clientOut.println("Unknown");
                        continue;
                    }
                    BufferedReader in = new BufferedReader(new FileReader(f));
                    clientOut.println(in.readLine());
                    in.close();
                } else if(splitted[0].equals("exit")) {
                    clientSocket.close();
                    break;
                } else {
                    clientOut.println("Error: unknown server command.");
                }
            }
        } catch(IOException e) {
            // The clientSocket has probably been closed.
        } finally {
            try {
                clientIn.close();
                clientOut.close();
                clientSocket.close();
            } catch(IOException e) {
                // Assume the socket is already closed.
            }
            socketHandler.removeConnection(this);
        }
    }

    /**
     * This is the handler for when the getBusTimesByStopCode:stopCode message
     * is received by the server. It contacts the Bus Tracker HTTP website to
     * retrieve the data for that stop and transforms that data in to JSON
     * representation and replies to the client.
     *
     * @param stopCode The stop code argument supplied in the call from the
     * client.
     */
    private void getBusTimesByStopCode(final String stopCode) {
        if(stopCode == null) throw new IllegalArgumentException("The stop " +
                "code must not be null.");
        if(stopCode.length() == 0) throw new IllegalArgumentException("The " +
                "length of the stop code must not be 0.");

        try {
            URL url = new URL(Config.getConfig().getMainWebsiteURL() +
                    "getBusStopDepartures.php?refreshCount=0&clientType=" +
                    "b&busStopCode=" + stopCode + "&busStopDay=0&" +
                    "busStopService=0&numberOfPassage=4&busStopTime=&" +
                    "busStopDestination=0");
            HttpURLConnection connection =
                    (HttpURLConnection)url.openConnection();
            XMLReader parser = XMLReaderFactory.createXMLReader();
            ContentHandler busStopData = new LiveBusStopData();
            parser.setContentHandler(busStopData);
            InputStream in = connection.getInputStream();
            InputSource source = new InputSource(in);
            parser.parse(source);
            in.close();
            connection.disconnect();
            LiveBusStopData stopData = (LiveBusStopData)busStopData;
            clientOut.println("+");
            stopData.writeJSONToStream(clientOut);
            clientOut.println();
            clientOut.println("-");
        } catch(MalformedURLException e) {
            System.err.println("The URL protocol was not recognised.");
        } catch(IOException e) {
            System.err.println("An IOException occurred during the connection" +
                    " to the web server. Error:");
            System.err.println(e.toString());
        } catch(SAXException e) {
            System.err.println("An error occurred while trying to initialise " +
                    "the XML parser.");
            System.err.println(e.toString());
        }
    }
}