package com.udacity.ferfig.wheresmygas.api;

import com.udacity.ferfig.wheresmygas.model.maps.GasStationsList;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.QueryMap;

public interface RetrofitClient {
    @GET("/maps/api/place/nearbysearch/json")
    Call<GasStationsList> getStations(@QueryMap Map<String, String> params);
}
