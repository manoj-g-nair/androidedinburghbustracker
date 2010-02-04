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

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

/**
 * The Config class deals with the configuration of the server. If initConfig()
 * is not called before attempting to retrieve values from the config, then
 * default config values will be assumed. The purpose of initConfig() is to read
 * the values from the specified config file.
 *
 * @author Niall Scott
 */
public class Config {

    private static Config config = null;

    private int portNumber = 4876;
    private String addressToBind = "0.0.0.0";
    private int maxConnections = 100;
    private String dbPath = "./";
    /** Read only. */
    private static final String WWW_SITE_URL = "http://www.mybustracker.co.uk/";

    /**
     * Construct and initialise the configuration. This is not a publically
     * accessible constructor to control the amount of instances of this class.
     *
     * @param configFile The file path to the config file.
     */
    private Config(final String configFile) {
        if(configFile == null) throw new IllegalArgumentException("The " +
                "configFile must not be null.");
        if(configFile.length() == 0) throw new IllegalArgumentException("The " +
                "configFile must have a length greater than 0.");

        int lineNumber = 0;
        try {
            BufferedReader reader = new BufferedReader(
                    new FileReader(configFile));

            String input = "";
            String[] keyValue;
            while((input = reader.readLine()) != null) {
                lineNumber++;
                input = input.trim();
                if(input.length() == 0) continue;
                if(input.startsWith("#")) continue;
                // Key is on left, value is on right
                keyValue = input.split("=");
                if(keyValue.length < 2) continue;
                if(keyValue[0].trim().toLowerCase().equals("port")) {
                    try {
                        int tmp = Integer.parseInt(keyValue[1].trim());
                        if(tmp < 1 || tmp > 65535) {
                            System.err.println("The port range is 1 to 65535.");
                        } else {
                            portNumber = tmp;
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("The port number specified is not " +
                                "a valid integer number.");
                    }
                } else if(keyValue[0].trim().toLowerCase().equals(
                        "bindaddress")) {
                    addressToBind = keyValue[1].trim();
                } else if(keyValue[0].trim().toLowerCase().equals(
                        "maxconnections")) {
                    int tmp = Integer.parseInt(keyValue[1].trim());
                    if(tmp < 0) System.err.println("The maximum number of "
                            + "connections must not be less than 0.");
                } else if(keyValue[0].trim().toLowerCase().equals("dbpath")) {
                    dbPath = keyValue[1].trim();
                    dbPath = addressToBind.replace('/', File.pathSeparatorChar);
                    dbPath = addressToBind.replace('\\',
                            File.pathSeparatorChar);
                } else {
                    System.err.println("Invalid config key \"" + keyValue[0] +
                            "\".");
                }
            }
            reader.close();
        } catch (IOException e) {
            System.err.println("The following IOException occurred while " +
                    "dealing with the config file on line number " +
                    lineNumber + ": ");
            System.err.println(e.toString());
            System.err.println("The server config is now at internal " +
                    "defaults due to the exception.");
        }
    }
    
    /**
     * This constructor does not read from a config file, instead it uses the
     * default values.
     */
    private Config() {
        // Use preconfigured defaults
    }

    /**
     * Initialise the configuration.
     *
     * @param configFile The path to the config file.
     * @return An instance of the config manager.
     */
    public static Config initConfig(final String configFile) {
        if(config == null) config = new Config(configFile);
        return config;
    }

    /**
     * Get the instance to the config manager. If the config manager doesn't
     * already exist, create a default config.
     *
     * @return An instance of the config manager.
     */
    public static Config getConfig() {
        if(config == null) config = new Config();
            return config;
    }

    /**
     * Get the port number that is currently configured.
     *
     * @return The port number currently configured.
     */
    public int getPortNumber() {
        return portNumber;
    }

    /**
     * Get the address to bind to on the system.
     *
     * @return The address to bind to on the system.
     */
    public String getAddressToBind() {
        return addressToBind;
    }

    /**
     * Get the max number of connections to the server.
     *
     * @return The max number of connections to the server.
     */
    public int getMaxConnections() {
        return maxConnections;
    }

    /**
     * Get the URL of the main bus tracker website.
     *
     * @return The URL of the main bus tracker website.
     */
    public String getMainWebsiteURL() {
        return WWW_SITE_URL;
    }

    /**
     * Get the directory path where the bus stop location database should be
     * put after being created.
     *
     * @return The directory path of where the bus stop location database should
     * be put.
     */
    public String getDBPath() {
        return dbPath;
    }
}