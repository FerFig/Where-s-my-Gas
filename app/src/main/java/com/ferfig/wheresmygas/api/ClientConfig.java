package com.ferfig.wheresmygas.api;

public class ClientConfig {
    public static final String BASE_URL = "https://maps.googleapis.com";

    public static final String PARAM_LOCATION = "location";
    public static final String PARAM_RADIUS = "radius";
    public static final String PARAM_TYPE = "type";
    public static final String PARAM_TYPE_VALUE = "gas_station";
    public static final String PARAM_KEY = "key";

    private ClientConfig() {}

    public static String formatParamLocation(double latitude, double longitude){
        return latitude + ","+ longitude;
    }
}
