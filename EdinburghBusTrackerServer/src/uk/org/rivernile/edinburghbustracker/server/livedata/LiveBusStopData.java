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

package uk.org.rivernile.edinburghbustracker.server.livedata;

import java.util.ArrayList;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This class holds the live bus stop data. It gets the values from the parsed
 * XML file and stores them in a structure which can be retrieved at a later
 * time.
 *
 * @author Niall Scott
 */
public class LiveBusStopData extends DefaultHandler {

    private String thisStopCode = "";
    private String thisStopName = "";
    private String route;
    private boolean stopInformation = false;
    private boolean busInformation = false;
    private LiveBus bus;
    private BusService service;

    private ArrayList<BusService> busServices;

    /**
     * Create a new instance of LiveBusStopData.
     */
    public LiveBusStopData() {
        super();

        busServices = new ArrayList<BusService>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startElement(String uri, String localName, String qname,
            Attributes attributes) {
        if(localName.toLowerCase().equals("a")) {
            stopInformation = true;
        } else if(localName.toLowerCase().equals("pre")) {
            busInformation = true;
        } else if(localName.toLowerCase().equals("span")) {
            if(bus != null) bus.setAccessible(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endElement(String uri, String localName, String qName) {
        if(localName.toLowerCase().equals("a")) {
            stopInformation = false;
        } else if(localName.toLowerCase().equals("pre")) {
            busInformation = false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void characters(char[] ch, int start, int length) {
        StringBuffer sb = new StringBuffer();
        String s;
        for(int i = start; i < start + length; i++) {
            if(ch[i] == ' ' && i > start) {
                if(ch[i-1] != ' ') {
                    sb.append(ch[i]);
                }
            } else {
                sb.append(ch[i]);
            }
        }
        s = sb.toString().trim();
        if(stopInformation) {
            handleStopInformation(s);
        } else if(busInformation) {
            handleBusInformation(s);
        }
    }

    /**
     * Handle the information regarding a bus service's route.
     *
     * @param infoLine The string to parse.
     */
    private void handleStopInformation(String infoLine) {
        char[] chars = infoLine.toCharArray();
        int stage = 0;

        service = new BusService();
        busServices.add(service);
        route = "";
        thisStopCode = "";
        thisStopName = "";
        for(int i = 0; i < chars.length; i++) {
            switch(stage) {
                case 0:
                    if(chars[i] == ' ') {
                        thisStopCode = thisStopCode.trim();
                        stage++;
                    } else {
                        thisStopCode = thisStopCode + chars[i];
                    }
                    break;
                case 1:
                    if(chars[i] == '/') {
                        thisStopCode = thisStopCode.trim();
                        stage++;
                    } else {
                        thisStopName = thisStopName + chars[i];
                    }
                    break;
                case 2:
                    route = route + chars[i];
                    break;
                default:
                    break;
            }
        }
        route = route.trim();
        service.setRoute(route);
    }

    /**
     * Handle the information regarding when a bus is due at a stop and it's
     * destination.
     *
     * @param infoLine The string to parse.
     */
    private void handleBusInformation(String infoLine) {
        bus = new LiveBus();
        String[] splitted = infoLine.split("\\s+");
        if(splitted.length < 3) return;
        service.setServiceName(splitted[0].trim());
        bus.setArrivalTime(splitted[splitted.length-1].trim());
        if(splitted.length == 3) {
            bus.setDestination(splitted[1].trim());
        } else {
            String dest = splitted[1];
            for(int i = 2; i < splitted.length-1; i++) {
                dest = dest + " " + splitted[i];
            }
            bus.setDestination(dest);
        }
        service.addLiveBus(bus);
    }

    public void test() {
        System.out.println("Stop Code: " + thisStopCode);
        System.out.println("Stop Name: " + thisStopName);
        for(BusService s : busServices) {
            System.out.println(s.getServiceName() + " " + s.getRoute());
            for(LiveBus b : s.buses) {
                System.out.print(s.getServiceName() + " " +
                        b.getDestination() + " ");
                if(b.getAccessible()) System.out.print("WHEELCHAIR ");
                System.out.println(b.getArrivalTime());
            }
        }
    }
}