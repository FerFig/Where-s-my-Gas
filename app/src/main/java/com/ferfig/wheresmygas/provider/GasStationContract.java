package com.ferfig.wheresmygas.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public class GasStationContract {
    // The authority, which is how your code knows which Content Provider to access
    public static final String AUTHORITY = "com.ferfig.wheresmygas";

    // The base content URI = "content://" + <authority>
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    // Define the possible paths for accessing data in this contract
    // This is the path for the "gas_stations" directory
    public static final String PATH_GAS_STATIONS = "gas_stations";

    private GasStationContract() {}

    public static final class GasStationEntry implements BaseColumns {

        private GasStationEntry() {}

        // TaskEntry content URI = base content URI + path
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_GAS_STATIONS).build();

        // table name
        public static final String TABLE_NAME = "gas_stations";

        // columns
        public static final String COLUMN_GAS_STATION_ID = "id";
        public static final String COLUMN_GAS_STATION_NAME = "name";
        public static final String COLUMN_GAS_STATION_IMAGE_URL = "image";
        public static final String COLUMN_GAS_STATION_LATITUDE = "latitude";
        public static final String COLUMN_GAS_STATION_LONGITUDE = "longitude";
        public static final String COLUMN_GAS_STATION_ADDRESS = "address";
        public static final String COLUMN_GAS_STATION_DETAILS = "details";
    }
}
