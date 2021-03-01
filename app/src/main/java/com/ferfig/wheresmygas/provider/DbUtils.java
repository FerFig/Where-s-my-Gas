package com.ferfig.wheresmygas.provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import com.ferfig.wheresmygas.model.GasStationTypeConverter;
import com.ferfig.wheresmygas.model.GasStation;
import com.ferfig.wheresmygas.provider.GasStationContract.GasStationEntry;

public class DbUtils {
    public static boolean addGasStationToDB(Context context, GasStation gasStation) {
        ContentValues cv = new ContentValues();

        cv.put(GasStationEntry.COLUMN_GAS_STATION_ID, gasStation.getPlaceId());
        cv.put(GasStationEntry.COLUMN_GAS_STATION_NAME, gasStation.getName());
        cv.put(GasStationEntry.COLUMN_GAS_STATION_IMAGE_URL, gasStation.getImageUrl());
        cv.put(GasStationEntry.COLUMN_GAS_STATION_LATITUDE, gasStation.getLatitude());
        cv.put(GasStationEntry.COLUMN_GAS_STATION_LONGITUDE, gasStation.getLongitude());
        cv.put(GasStationEntry.COLUMN_GAS_STATION_ADDRESS, gasStation.getAddress());
        cv.put(GasStationEntry.COLUMN_GAS_STATION_DETAILS, GasStationTypeConverter.gasStationListToString(gasStation.getDetails()));

        ContentResolver cr = context.getContentResolver();
        Uri uriInserted = cr.insert(GasStationEntry.CONTENT_URI, cv);

        return (uriInserted!=null);
    }

    public static boolean deleteGasStationFromDB(Context context, GasStation gasStation) {
        ContentResolver cr = context.getContentResolver();
        String[] mSelectionArgs = {gasStation.getPlaceId()};
        int nDeleted = cr.delete(GasStationEntry.CONTENT_URI, GasStationEntry.COLUMN_GAS_STATION_ID + "= ?", mSelectionArgs);
        return nDeleted>0;
    }
}
