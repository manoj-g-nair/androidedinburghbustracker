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

/**
 * This is the main class of the application, which contains the entry point in
 * to the application and also the version details of the application.
 *
 * @author Niall Scott
 */
public final class Main {

    /** Major release number of application */
    public final static int VERSIONMAJOR = 0;
    /** Minor release number of application */
    public final static int VERSIONMINOR = 0;
    /** Release revision of application */
    public final static int VERSIONREVISION = 1;
    /** The title of the application */
    public final static String APPLICATIONTITLE = "Edinburgh Bus Tracker for " +
            "Android Server";
    /** The default file path of the config file */
    public static String configFile = "bustracker.conf";

    /**
     * The constructor is intentionally empty as this class should not be
     * initialised.
     */
    public Main() {

    }

    /**
     * The main execution entry point.
     *
     * @param args The command line arguments.
     */
    public static void main(String[] args) {
        if(args.length > 0) {
            if(args[0].trim().equals("-v")
                    || args[0].trim().toLowerCase().equals("--version"))
            {
                System.out.println(APPLICATIONTITLE);
                System.out.println("Version: " + getVersion());
            } else if(args[0].trim().equals("-h")
                    || args[0].trim().toLowerCase().equals("--help")) {
                printHelp();
            } else if(args[0].trim().equals("-c")
                    || args[0].trim().toLowerCase().equals("--config")) {
                if(args.length >= 2) {
                    configFile = args[1].trim();
                    start();
                } else {
                    System.out.println("You must specify a file path if you " +
                            "use the config flag.");
                }
            } else {
                System.out.println("Incorrect command line parameters.");
            }
        } else {
            start();
        }
    }

    /**
     * This is where the real work starts.
     */
    public static void start() {
        Config.initConfig(configFile);
        IncomingSocketHandler socketHandler = new IncomingSocketHandler();
        socketHandler.run();
        System.out.println("The server is now exiting.");
    }

    /**
     * Prints the help instructions to standard output.
     */
    public static void printHelp() {
        System.out.println("Usage: ./ebtserver [OPTIONS] [PARAMS]");
        System.out.println();
        System.out.printf("OPTION\tLONG OPTION\t\tMEANING\n");
        System.out.printf("-c\t--config\t\tUse the config file specified in " +
                "the next parameter.\n");
        System.out.printf("-h\t--help\t\t\tThis help text.\n");
        System.out.printf("-v\t--version\t\tThe server version.\n");
    }

    /**
     * Get the version of the application, which is the concatination of the
     * VERSIONMAJOR, VERSIONMINOR and VERSIONREVISION static variables in this
     * class.
     *
     * @return The version of the program in String form.
     */
    public final static String getVersion() {
        return VERSIONMAJOR + "." + VERSIONMINOR + "." + VERSIONREVISION;
    }

}