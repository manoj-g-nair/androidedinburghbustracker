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

package uk.org.rivernile.edinburghbustracker.android;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class BusStopDatabase extends SQLiteOpenHelper {

    protected final static String STOP_DB_NAME = "busstops.db";
    protected final static int STOP_DB_VERSION = 1;

    private static BusStopDatabase instance = null;

    private BusStopDatabase(final Context context) {
        super(context, STOP_DB_NAME, null, STOP_DB_VERSION);
    }

    public static BusStopDatabase getInstance(final Context context) {
        if(instance == null) instance = new BusStopDatabase(context);
        return instance;
    }

    @Override
    protected void finalize() {
        getReadableDatabase().close();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // The database should already exist, do nothing if it doesn't.
    }

    @Override
    public void onUpgrade(final SQLiteDatabase db, final int oldVersion,
            final int newVersion)
    {
        // Do nothing.
    }

    public Cursor getBusStopsByCoords(final int minX, final int minY,
            final int maxX, final int maxY)
    {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query("bus_stops", null, "x <= " + minX + " AND y >= " +
                minY + " AND x >= " + maxX + " AND y <= " + maxY, null, null,
                null, null);
        return c;
    }

    public String[] getBusServicesForStop(final String stopCode) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(true, "service_stops",
                new String[] { "serviceName" }, "stopCode = " + stopCode, null,
                null, null, "serviceName ASC", null);
        int count = c.getCount();
        int i = 0;
        if(count > 0) {
            String[] result = new String[count];
            while(!c.isLast()) {
                c.moveToNext();
                result[i] = c.getString(0);
                i++;
            }
            c.close();
            return result;
        } else {
            c.close();
            return null;
        }
    }

    public long getLastDBModTime() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(true, "metadata", new String[] { "updateTS" }, null,
                null, null, null, null, null);
        if(c.getCount() == 0) return 0;
        c.moveToNext();
        String result = c.getString(0);
        c.close();
        try {
            return Long.parseLong(result);
        } catch(NumberFormatException e) {
            return 0;
        }
    }
}