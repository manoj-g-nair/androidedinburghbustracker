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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * The Database class deals with the connection and transaction handling of the
 * SQLite database which holds the bus stop location information. This database
 * eventually ends up on Android clients. Much of this class is for convenience.
 * Note, only one instance of this class can be instantiated.
 *
 * @author Niall Scott
 */
public class Database {

    public static final String DB_FILE = "busstops.db";
    public static final String LAST_MOD_FILE = "dblastmod";
    private static final String TABLE_STOPS = "bus_stops";
    private static final String TABLE_SERVICES = "service_stops";
    private static final String TABLE_METADATA = "metadata";

    private static Database db;

    private Connection con;
    private Statement stmt;
    private PreparedStatement insrtStops, insrtServices;

    /**
     * This constuctor is private and can only be called from getDatabase().
     * It sets up the connection to the database and does a little
     * initialisation.
     *
     * @throws ClassNotFoundException When the SQLite JDBC driver cannot be
     * found.
     * @throws SQLException When a problem occurs whilst trying to access the
     * SQLite database.
     */
    private Database() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        con = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE);
        con.setAutoCommit(false);
        stmt = con.createStatement();
        stmt.executeUpdate("DROP TABLE IF EXISTS " + TABLE_STOPS + ";");
        stmt.executeUpdate("DROP TABLE IF EXISTS " + TABLE_SERVICES + ";");
        stmt.executeUpdate("DROP TABLE IF EXISTS " + TABLE_METADATA + ";");
        stmt.executeUpdate("CREATE TABLE " + TABLE_STOPS + " (_id TEXT " +
                "PRIMARY KEY, stopName TEXT, x INTEGER, y INTEGER);");
        stmt.executeUpdate("CREATE TABLE " + TABLE_SERVICES + " (_id INTEGER " +
                "PRIMARY KEY AUTOINCREMENT, stopCode TEXT, serviceName TEXT);");
        stmt.executeUpdate("CREATE TABLE " + TABLE_METADATA + " (_id INTEGER " +
                "PRIMARY KEY AUTOINCREMENT, updateTS TEXT);");
        insrtStops = con.prepareStatement("INSERT INTO " + TABLE_STOPS +
                " VALUES (?, ?, ?, ?);");
        insrtServices = con.prepareStatement("INSERT INTO " + TABLE_SERVICES +
                " (stopCode, serviceName) VALUES (?, ?);");
    }

    /**
     * Get the single instance of the Database class.
     *
     * @return The single instance of the Database class.
     * @throws ClassNotFoundException When the SQLite JDBC driver cannot be
     * found.
     * @throws SQLException When a problem occurs whilst trying to access the
     * SQLite database.
     */
    public static Database getDatabase() throws ClassNotFoundException,
            SQLException
    {
        if(db == null) db = new Database();
        return db;
    }

    /**
     * Insert a new bus stop and its location in to the database.
     *
     * @param stopCode The stop code of the bus stop.
     * @param stopName The name of the bus stop.
     * @param x The x position of the bus stop.
     * @param y The y position of the bus stop.
     * @throws SQLException When a problem occurs whilst trying to access the
     * SQLite database.
     */
    public void insertStop(final String stopCode, final String stopName,
            final int x, final int y) throws SQLException
    {
        if(stopCode == null || stopCode.length() < 1)
            throw new IllegalArgumentException("The stopCode parameter must " +
                    "not be null or blank.");
        if(stopName == null || stopName.length() < 1)
            throw new IllegalArgumentException("The stopName parameter must " +
                    "not be null or blank");

        insrtStops.setString(1, stopCode);
        insrtStops.setString(2, stopName);
        insrtStops.setInt(3, x);
        insrtStops.setInt(4, y);
        insrtStops.addBatch();
    }

    /**
     * Insert an association between a bus service and a bus stop in to the
     * database.
     *
     * @param stopCode The stop code of the bus stop.
     * @param serviceName The name of the bus service.
     * @throws SQLException When a problem occurs whilst trying to access the
     * SQLite database.
     */
    public void insertService(final String stopCode, final String serviceName)
            throws SQLException
    {
        if(stopCode == null || stopCode.length() < 1)
            throw new IllegalArgumentException("The stopCode parameter must " +
                    "not be null or blank.");
        if(serviceName == null || serviceName.length() < 1)
            throw new IllegalArgumentException("The serviceName parameter " +
                    "must not be null or blank.");
        insrtServices.setString(1, stopCode);
        insrtServices.setString(2, serviceName);
        insrtServices.addBatch();
    }

    /**
     * This method SHOULD be called when the activity using this class has
     * finished dealing with the database.
     *
     * @throws SQLException When a problem occurs whilst trying to access the
     * SQLite database.
     */
    public void finished() throws SQLException{
        insrtStops.executeBatch();
        insrtServices.executeBatch();
        long currentTS = System.currentTimeMillis();
        stmt.executeUpdate("INSERT INTO " + TABLE_METADATA + " (updateTS) " +
                "VALUES (\"" + currentTS + "\");");
        con.commit();
        try {
            BufferedWriter out = new BufferedWriter(
                    new FileWriter(LAST_MOD_FILE));
            out.write("" + currentTS);
            out.close();
        } catch(IOException e) {
            // Do nothing.
        }
        try {
            db = null;
            con.close();
        } catch(SQLException e) {
            // Do nothing as we're closing anyway
        }
    }
}