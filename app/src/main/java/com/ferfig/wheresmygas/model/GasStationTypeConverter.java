package com.ferfig.wheresmygas.model;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ferfig.wheresmygas.model.maps.Result;

import java.lang.reflect.Type;


public class GasStationTypeConverter {
    private static final Gson gson = new Gson();

    private GasStationTypeConverter() {}

    /*** Converter for Gas Station details **/
    public static Result stringToGasStationList(String data) {
        if (data == null) {
            return new Result(); //return empty result
        }

        Type listType = new TypeToken<Result>() {
        }.getType();

        return gson.fromJson(data, listType);
    }

    public static String gasStationListToString(Result gasStationDetails) {
        return gson.toJson(gasStationDetails);
    }
}
