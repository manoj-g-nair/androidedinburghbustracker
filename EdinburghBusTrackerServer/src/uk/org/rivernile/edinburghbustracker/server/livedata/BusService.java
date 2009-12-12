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

/**
 * This class holds information on a bus service and not just a single bus.
 *
 * @author Niall Scott
 */
public class BusService {

    private String serviceName;
    private String route;

    protected ArrayList<LiveBus> buses;

    /**
     * Create a new BusService instance.
     */
    public BusService() {
        buses = new ArrayList<LiveBus>();
    }

    /**
     * Set the name of this bus service.
     *
     * @param serviceName The name of this bus service.
     */
    public void setServiceName(final String serviceName) {
        if(serviceName == null) throw new IllegalArgumentException("The " +
                "service name must not be null.");
        if(serviceName.length() == 0) throw new IllegalArgumentException("The" +
                " service name length must not be 0.");
        this.serviceName = serviceName;
    }

    /**
     * Set the route of the bus service.
     *
     * @param origin The route of the bus service.
     */
    public void setRoute(final String route) {
        if(route == null) throw new IllegalArgumentException("The origin " +
                "must not be null.");
        if(route.length() == 0) throw new IllegalArgumentException("The " +
                "origin length must not be 0.");
        this.route = route;
    }

    /**
     * Get the name of this bus service.
     *
     * @return The name of this bus service.
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * Get the route of this bus service.
     *
     * @return The route of this bus service.
     */
    public String getRoute() {
        return route;
    }

    /**
     * Add a new live bus for this service.
     *
     * @param liveBus The live bus to add for this service.
     */
    public void addLiveBus(final LiveBus liveBus) {
        if(liveBus == null) throw new IllegalArgumentException("The live bus " +
                "cannot be null.");
        buses.add(liveBus);
    }
}