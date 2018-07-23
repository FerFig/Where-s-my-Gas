package com.udacity.ferfig.wheresmygas.Api;

public class ClientConfig {
    public static final String BASE_URL = "https://maps.googleapis.com";

    public static final String paramLocation = "location";
    public static final String paramRadius = "radius";
    public static final String paramType = "type";
    public static final String paramTypeValue = "gas_station";
    public static final String paramKey = "key";

    public static String formatParamLocation(Double latitude, double longitude){
        return latitude + ","+ longitude;
    }
}
