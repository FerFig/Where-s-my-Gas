package com.udacity.ferfig.wheresmygas.provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import com.udacity.ferfig.wheresmygas.model.GasStation;
import com.udacity.ferfig.wheresmygas.provider.GasStationContract.GasStationEntry;

public class DbUtils {
    public static boolean addGasStationToDB(Context context, GasStation gasStation) {
        ContentValues cv = new ContentValues();
        cv.put(GasStationEntry.COLUMN_GAS_STATION_NAME, gasStation.getName());
        cv.put(GasStationEntry.COLUMN_GAS_STATION_IMAGE_URL, gasStation.getImageUrl());
        cv.put(GasStationEntry.COLUMN_GAS_STATION_LATITUDE, gasStation.getLatitude());
        cv.put(GasStationEntry.COLUMN_GAS_STATION_LONGITUDE, gasStation.getLongitude());
        cv.put(GasStationEntry.COLUMN_GAS_STATION_DISTANCE, gasStation.getDistance());
        cv.put(GasStationEntry.COLUMN_GAS_STATION_DETAILS, gasStation.getDetails());

        ContentResolver cr = context.getContentResolver();
        Uri uriInserted = cr.insert(GasStationEntry.CONTENT_URI, cv);

        return (uriInserted!=null);
    }

    public static boolean deleteGasStationFromDB(Context context, long gasStationId) {
        ContentResolver cr = context.getContentResolver();
        Uri movieToDelete = GasStationEntry.buildGasStationUri(gasStationId);
        int nDeleted = cr.delete(movieToDelete, null, null);
        return nDeleted>0;
    }
}
