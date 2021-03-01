package com.ferfig.wheresmygas.provider;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ferfig.wheresmygas.model.GasStationTypeConverter;
import com.ferfig.wheresmygas.model.GasStation;
import com.ferfig.wheresmygas.provider.GasStationContract.GasStationEntry;

import java.util.ArrayList;

public class GasStationsAsyncLoader extends GasStationsAsyncTaskLoader<ArrayList<GasStation>> {
    public static final int LOADER_ID = 27;

    private final ContentResolver mContentResolver;

    public GasStationsAsyncLoader(@NonNull Context context, ContentResolver contentResolver) {
        super(context);
        mContentResolver = contentResolver;
    }

    @Nullable
    @Override
    public ArrayList<GasStation> loadInBackground() {
        try {
            if (this.getId() == LOADER_ID) {
                Cursor cursor = mContentResolver.query(
                        GasStationEntry.CONTENT_URI, null, null, null, null);
                ArrayList<GasStation> mAsyncGasStationList = new ArrayList<>();
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        GasStation gasStation = new GasStation(
                                cursor.getString(cursor.getColumnIndex(GasStationEntry.COLUMN_GAS_STATION_ID)),
                                cursor.getString(cursor.getColumnIndex(GasStationEntry.COLUMN_GAS_STATION_NAME)),
                                cursor.getString(cursor.getColumnIndex(GasStationEntry.COLUMN_GAS_STATION_IMAGE_URL)),
                                cursor.getDouble(cursor.getColumnIndex(GasStationEntry.COLUMN_GAS_STATION_LATITUDE)),
                                cursor.getDouble(cursor.getColumnIndex(GasStationEntry.COLUMN_GAS_STATION_LONGITUDE)),
                                cursor.getString(cursor.getColumnIndex(GasStationEntry.COLUMN_GAS_STATION_ADDRESS)),
                                GasStationTypeConverter.stringToGasStationList(
                                    cursor.getString(cursor.getColumnIndex(GasStationEntry.COLUMN_GAS_STATION_DETAILS))
                                )
                        );
                        mAsyncGasStationList.add(gasStation);
                    }
                    cursor.close();
                }
                return mAsyncGasStationList;
            }
            return null;
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }
}
