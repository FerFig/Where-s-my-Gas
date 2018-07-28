package com.udacity.ferfig.wheresmygas.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.udacity.ferfig.wheresmygas.provider.GasStationContract.GasStationEntry;

public class GasStationDbHelper extends SQLiteOpenHelper {
    // DB name & version
    private static final String DATABASE_NAME = "wheresmygas.db";
    // If database schema changed, must increment the database version
    private static final int DATABASE_VERSION = 1;

    GasStationDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create a table to hold the gas stations data
        final String SQL_CREATE_TABLE = "CREATE TABLE " + GasStationEntry.TABLE_NAME + " (" +
                GasStationEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                GasStationEntry.COLUMN_GAS_STATION_NAME + " TEXT NOT NULL, " +
                GasStationEntry.COLUMN_GAS_STATION_IMAGE_URL + " TEXT, " +
                GasStationEntry.COLUMN_GAS_STATION_LATITUDE + " REAL NOT NULL, " +
                GasStationEntry.COLUMN_GAS_STATION_LONGITUDE + " REAL NOT NULL, " +
                GasStationEntry.COLUMN_GAS_STATION_DISTANCE + " REAL, " +
                GasStationEntry.COLUMN_GAS_STATION_DETAILS + " TEXT " +
                ");";

        db.execSQL(SQL_CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //TODO: improve upgrade to avoid loose favorite gas stations
        // For now simply drop the table and create a new one.
        db.execSQL("DROP TABLE IF EXISTS " + GasStationEntry.TABLE_NAME);
        onCreate(db);

    }
}
