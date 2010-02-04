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

package uk.org.rivernile.edinburghbustracker.server.stoplocations;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * This is class which deals with maintaining a database of stop locations and
 * what services serve these stops.
 * 
 * @author Niall Scott
 */
public class StopLocationsTask extends DefaultHandler {

    private String dbPath;
    private String currStopCode;
    private String currStopName;
    private String currX;
    private String currY;
    private String currServiceName, loopServiceName;
    private boolean onStopCode = false, onStopName = false, onX = false,
            onY = false, onServiceName = false, ignoreRest = false;
    private LinkedList<String> services, stops;
    private Database db;
    private Timer timer;

    /**
     * Initialise the stop locations task. The object created will attempt to
     * create a database if one does not already exist, will update the database
     * if it more than a week old and will start a TimerTask which automatically
     * updates the database every Sunday at 06:00:00.
     *
     * @param dbPath The path where the database should be stored.
     */
    public StopLocationsTask(final String dbPath) {
        if(dbPath == null) throw new IllegalArgumentException("The dbPath " +
                "must not be null.");
        if(dbPath.length() < 1) throw new IllegalArgumentException("The " +
                "length of dbPath must be at least 1.");
        this.dbPath = dbPath;
        new Thread(task).start();
        timer = new Timer();
        Calendar date = Calendar.getInstance();
        date.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        date.set(Calendar.HOUR, 6);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        timer.schedule(tt, date.getTime(), 604800000);
    }

    private Runnable task = new Runnable() {
        @Override
        public void run() {
            File f = new File(dbPath + Database.DB_FILE);
            if(!f.exists()) {
                doTask();
                new File(Database.DB_FILE).renameTo(f);
            } else {
                if(f.lastModified() >= 604800000) {
                    doTask();
                    new File(Database.DB_FILE).renameTo(f);
                }
            }
        }
    };

    private TimerTask tt = new TimerTask() {
        @Override
        public void run() {
            File orig = new File(Database.DB_FILE);
            File dest = new File(dbPath + Database.DB_FILE);
            if(orig.exists()) orig.delete();
            doTask();
            if(dest.exists()) dest.delete();
            orig.renameTo(dest);
        }
    };

    /**
     * The database fecthing task.
     */
    private void doTask() {
        services = new LinkedList<String>();
        stops = new LinkedList<String>();
        loopServiceName = "1";
        services.add(loopServiceName);
        try {
            db = Database.getDatabase();
        } catch(SQLException e) {
            System.err.println("The following SQLException occurred:");
            System.err.println(e.toString());
            System.err.println("The stop location fetching loop cannot " +
                    "continue.");
            return;
        } catch(ClassNotFoundException e) {
            System.err.println("The SQLite JDBC driver could not be found, " +
                    "the stop location fetching loop cannot continue.");
            return;
        }

        int i = 0;
        URL url;
        HttpURLConnection con;
        InputStream in;
        InputSource source;
        XMLReader parser;
        try {
            parser = XMLReaderFactory.createXMLReader();
            parser.setContentHandler(this);
        } catch(SAXException e) {
            System.err.println("An exception occurred while trying to " +
                    "initialise the XML parser:");
            System.err.println(e.toString());
            return;
        }
        while(i < services.size()) {
            loopServiceName = services.get(i);
            try {
                url = new URL("http://www.mybustracker.co.uk/" +
                        "getServicePoints.php?serviceMnemo=" + services.get(i));
                con = (HttpURLConnection)url.openConnection();
                in = con.getInputStream();
                source = new InputSource(in);
                ignoreRest = false;
                parser.parse(source);
                in.close();
                con.disconnect();
                i++;
            } catch(MalformedURLException e) {
                System.err.println(e.toString());
            } catch(IOException e) {
                System.err.println("The following IOException occurred whilst" +
                        "trying to get the stop location XML file from the " +
                        "Bustracker web server:");
                System.err.println(e.toString());
            } catch(SAXException e) {
                System.err.println("An exception occurred while trying to " +
                        "parse an XML file:");
                System.err.println(e.toString());
            }
        }
        try {
            db.finished();
        } catch(SQLException e) {
            System.err.println("Cannot commit changes to database due to " +
                    "SQLException:");
            System.err.println(e.toString());
        }
        db = null;
        System.out.println("Database now ready.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startElement(final String uri, final String localName,
            final String qname, final Attributes attributes)
    {
        if(ignoreRest) return;
        if(localName.toLowerCase().equals("sms")) {
            onStopCode = true;
        } else if(localName.toLowerCase().equals("nom")) {
            onStopName = true;
        } else if(localName.toLowerCase().equals("x")) {
            onX = true;
        } else if(localName.toLowerCase().equals("y")) {
            onY = true;
        } else if(localName.toLowerCase().equals("mnemo")) {
            onServiceName = true;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endElement(final String uri, final String localName,
            final String qName)
    {
        if(ignoreRest) return;
        if(localName.toLowerCase().equals("sms")) {
            onStopCode = false;
        } else if(localName.toLowerCase().equals("nom")) {
            onStopName = false;
        } else if(localName.toLowerCase().equals("x")) {
            onX = false;
        } else if(localName.toLowerCase().equals("y")) {
            onY = false;
        } else if(localName.toLowerCase().equals("mnemo")) {
            onServiceName = false;
            if(!services.contains(currServiceName)) {
                services.add(currServiceName);
            }
        } else if(localName.toLowerCase().equals("busstop")) {
            if(!stops.contains(currStopCode)) {
                try {
                    db.insertStop(currStopCode, currStopName, currX, currY);
                } catch(SQLException e) {
                    System.err.println("An SQLException occurred.");
                    System.err.println(e.toString());
                }
                stops.add(currStopCode);
            }
            try {
                db.insertService(currStopCode, loopServiceName);
            } catch(SQLException e) {
                System.err.println("An SQLException occurred.");
                System.err.println(e.toString());
            }
        } else if(localName.toLowerCase().equals("markers")) {
            ignoreRest = true;
            onStopCode = onStopName = onX = onY = onServiceName = false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void characters(final char[] ch, final int start, final int length) {
        if(ignoreRest) return;
        if(onStopCode) {
            currStopCode = charactersToString(ch, start, length);
        } else if(onStopName) {
            currStopName = charactersToString(ch, start, length);
        } else if(onX) {
            currX = charactersToString(ch, start, length);
        } else if(onY) {
            currY = charactersToString(ch, start, length);
        } else if(onServiceName) {
            currServiceName = charactersToString(ch, start, length);
        }
    }

    /**
     * {@inheritDoc}
     */
    private String charactersToString(final char[] ch, final int start,
            final int length)
    {
        StringBuffer sb = new StringBuffer();
        for(int i = start; i < start + length; i++) {
            sb.append(ch[i]);
        }
        return sb.toString().trim();
    }
}