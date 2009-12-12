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

/**
 * The LiveBus class holds information on the arrival information for a single
 * bus.
 *
 * @author Niall Scott
 */
public class LiveBus {

    private String destination;
    private String arrivalTime;
    private boolean accessible = false;

    /**
     * Create a new LiveBus instance.
     */
    public LiveBus() {
    }

    /**
     * Set the destination of this bus. It will most be equal to the destination
     * for the BusService, but may be different if the bus is part route.
     *
     * @param destination The destination for this bus, eg "Riccarton".
     */
    public void setDestination(final String destination) {
        if(destination == null) throw new IllegalArgumentException("The " +
                "destination must not be null.");
        if(destination.length() == 0) throw new IllegalArgumentException("The" +
                " destination length must not be 0.");
        this.destination = destination;
    }

    /**
     * Set the arrival time for this bus. It may be the number of minutes until
     * the next bus or it could be an actual time.
     *
     * @param arrivalTime The arrival time for this bus.
     */
    public void setArrivalTime(final String arrivalTime) {
        if(arrivalTime == null) throw new IllegalArgumentException("The " +
                "arrival time must not be null.");
        if(arrivalTime.length() == 0) throw new IllegalArgumentException("The" +
                " arrival time length must not be 0.");
        this.arrivalTime = arrivalTime;
    }

    /**
     * Set whether a bus is wheelchair capable or not.
     *
     * @param accessible True if this bus is wheelchair accessible, false if
     * not.
     */
    public void setAccessible(final boolean accessible) {
        this.accessible = accessible;
    }

    /**
     * Get the destination of this bus.
     *
     * @return The destination of this bus.
     */
    public String getDestination() {
        return destination;
    }

    /**
     * Get the arrival time of this bus.
     *
     * @return The arrival time of this bus.
     */
    public String getArrivalTime() {
        return arrivalTime;
    }

    /**
     * Get whether this bus is wheelchair accessible or not.
     *
     * @return True if this bus is wheelchair accessible, false if not.
     */
    public boolean getAccessible() {
        return accessible;
    }
}