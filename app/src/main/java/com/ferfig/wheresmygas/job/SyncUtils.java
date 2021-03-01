package com.ferfig.wheresmygas.job;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;
import com.ferfig.wheresmygas.R;
import com.ferfig.wheresmygas.Utils;
import com.ferfig.wheresmygas.api.ClientConfig;
import com.ferfig.wheresmygas.api.RetrofitClient;
import com.ferfig.wheresmygas.model.GasStation;
import com.ferfig.wheresmygas.model.GasStationTypeConverter;
import com.ferfig.wheresmygas.model.maps.GasStationsList;
import com.ferfig.wheresmygas.model.maps.Result;
import com.ferfig.wheresmygas.provider.GasStationContract;
import com.ferfig.wheresmygas.ui.widget.WmgWidget;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SyncUtils {

    private static final String JOB_IDENTITY = "wmg_sync_job";

    private static boolean sInitialized;

    private static final int JOB_INTERVAL_MINUTES = 15;
    private static final int JOB_INTERVAL_SECONDS = (int) TimeUnit.MINUTES.toSeconds(JOB_INTERVAL_MINUTES);
    private static final int JOB_FLEXTIME_SECONDS = JOB_INTERVAL_SECONDS;

    private static long mSearchAreaRadius = Utils.MAP_DEFAULT_SEARCH_RADIUS;

    public static void scheduleUpdateService(Context context) {
        if (sInitialized) return;

        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));

        Job gasStationUpdateJob = dispatcher.newJobBuilder()
                // the JobService that will be called
                .setService(GasStationsUpdateService.class)
                // uniquely identifies the job
                .setTag(JOB_IDENTITY)
                // recurring job
                .setRecurring(true)
                // persist past a device reboot
                .setLifetime(Lifetime.FOREVER)
                // start between 0 and 15 minutes (900 seconds)
                .setTrigger(Trigger.executionWindow(JOB_INTERVAL_SECONDS,
                        JOB_INTERVAL_SECONDS + JOB_FLEXTIME_SECONDS))
                // overwrite an existing job with the same tag
                .setReplaceCurrent(true)
                // retry with exponential backoff
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                // constraints that need to be satisfied for the job to run
                .setConstraints(
                        // only run when the device as internet connection
                        Constraint.ON_ANY_NETWORK
                )
                .build();

        try {
            dispatcher.mustSchedule(gasStationUpdateJob);
        } catch (FirebaseJobDispatcher.ScheduleFailedException ex) {
            Log.d(Utils.TAG,  ex.getMessage());
        }

        sInitialized = true;
    }

    static void executeWheresMyGasJob(Context context) {
        // first we need the last know location to calculate current distances
        Location lastKnownLocation = Utils.getLastKnownLocation(context);
        if (lastKnownLocation == null) {
            // try to get it from shared prefs
            lastKnownLocation = SyncUtils.getLastLocationFromPreferences(context);
        }else{
            SyncUtils.saveLastLocationToPreferences(context, lastKnownLocation);
        }
        if (lastKnownLocation == null) return;

        GasStation favoriteGasStation = SyncUtils.getFavoriteGasStationFromLocalDB(context, lastKnownLocation);
        if (favoriteGasStation != null){
            SyncUtils.saveFavoriteGasStationToPreferences(context, favoriteGasStation);
        }
        else{
            SyncUtils.deleteFavoriteGasStation(context);
        }

        GasStation nearGasStation = SyncUtils.getNearGasStations(context, lastKnownLocation, false);
        if (nearGasStation == null) { // try wider search...
            for (int i = 0; i < 20; i++) { //...for 20 times
                nearGasStation = SyncUtils.getNearGasStations(context, lastKnownLocation, true);
                if (nearGasStation != null) {
                    break;
                }
            }
        }
        if (nearGasStation != null) {
            SyncUtils.saveNearGasStationToPreferences(context, nearGasStation);
        }
        else{
            SyncUtils.deleteNearGasStation(context);
        }

        SyncUtils.sendUpdateInfoToWidget(context);
    }

    public static GasStation getNearGasStationFromPreferences(Context context) {
        return getGasStationPref(context, Utils.PREF_NEAR_GAS_STATION_PREFIX);
    }

    public static GasStation getFavoriteGasStationFromPreferences(Context context) {
        return getGasStationPref(context, Utils.PREF_FAVORITE_GAS_STATION_PREFIX);
    }

    private static GasStation getGasStationPref(Context context, String preferencePrefix) {
        SharedPreferences prefs = context.getSharedPreferences(Utils.PREFS_NAME, 0);
        String gasStationId = prefs.getString(preferencePrefix + Utils.PREF_GAS_STATION_ID, "");
        String gasStationName = prefs.getString(preferencePrefix + Utils.PREF_GAS_STATION_NAME, "");
        String gasStationImageUrl = prefs.getString(preferencePrefix + Utils.PREF_GAS_STATION_IMAGE_URL, "");
        Double gasStationLatitude = Double.longBitsToDouble(
                prefs.getLong(preferencePrefix + Utils.PREF_GAS_STATION_LATITUDE, 0));
        Double gasStationLongitude = Double.longBitsToDouble(
                prefs.getLong(preferencePrefix + Utils.PREF_GAS_STATION_LONGITUDE, 0));
        String gasStationAddress = prefs.getString(preferencePrefix + Utils.PREF_GAS_STATION_ADDRESS, "");
        return new GasStation(gasStationId, gasStationName, gasStationImageUrl,
                gasStationLatitude, gasStationLongitude, gasStationAddress, null);
    }

    private static void saveGasStationPref(Context context, GasStation gasStation, String preferencePrefix) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(Utils.PREFS_NAME, 0).edit();
        prefs.putString(preferencePrefix + Utils.PREF_GAS_STATION_ID, gasStation.getPlaceId());
        prefs.putString(preferencePrefix + Utils.PREF_GAS_STATION_NAME, gasStation.getName());
        prefs.putString(preferencePrefix + Utils.PREF_GAS_STATION_IMAGE_URL, gasStation.getImageUrl());
        prefs.putLong(preferencePrefix + Utils.PREF_GAS_STATION_LATITUDE,
                Double.doubleToRawLongBits(gasStation.getLatitude()));
        prefs.putLong(preferencePrefix + Utils.PREF_GAS_STATION_LONGITUDE,
                Double.doubleToRawLongBits(gasStation.getLongitude()));
        prefs.putString(preferencePrefix + Utils.PREF_GAS_STATION_ADDRESS, gasStation.getAddress());
        prefs.apply();
    }

    private static void saveNearGasStationToPreferences(Context context, GasStation gasStation) {
        saveGasStationPref(context, gasStation, Utils.PREF_NEAR_GAS_STATION_PREFIX);
    }

    private static void saveFavoriteGasStationToPreferences(Context context, GasStation gasStation) {
        saveGasStationPref(context, gasStation, Utils.PREF_FAVORITE_GAS_STATION_PREFIX);
    }

    public static Location getLastLocationFromPreferences(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Utils.PREFS_NAME, 0);
        Double gasStationLatitude = Double.longBitsToDouble(
                prefs.getLong(Utils.PREF_LAST_KNOWN_LATITUDE, 0));
        Double gasStationLongitude = Double.longBitsToDouble(
                prefs.getLong(Utils.PREF_LAST_KNOWN_LONGITUDE, 0));
        Location location = new Location(LocationManager.GPS_PROVIDER);
        location.setLatitude(gasStationLatitude);
        location.setLongitude(gasStationLongitude);
        return location;
    }

    public static void saveLastLocationToPreferences(Context context, Location location) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(Utils.PREFS_NAME, 0).edit();
        prefs.putLong(Utils.PREF_LAST_KNOWN_LATITUDE,
                Double.doubleToRawLongBits(location.getLatitude()));
        prefs.putLong(Utils.PREF_LAST_KNOWN_LONGITUDE,
                Double.doubleToRawLongBits(location.getLongitude()));
        prefs.apply();
    }

    public static void invalidateLastKnownLocation(Context context) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(Utils.PREFS_NAME, 0).edit();
        prefs.remove(Utils.PREF_LAST_KNOWN_LATITUDE);
        prefs.remove(Utils.PREF_LAST_KNOWN_LONGITUDE);
        prefs.apply();
    }

    private static void deleteGasStationPref(Context context, String preferencePrefix) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(Utils.PREFS_NAME, 0).edit();
        prefs.remove(preferencePrefix + Utils.PREF_GAS_STATION_ID);
        prefs.remove(preferencePrefix + Utils.PREF_GAS_STATION_NAME);
        prefs.remove(preferencePrefix + Utils.PREF_GAS_STATION_IMAGE_URL);
        prefs.remove(preferencePrefix + Utils.PREF_GAS_STATION_LATITUDE);
        prefs.remove(preferencePrefix + Utils.PREF_GAS_STATION_LONGITUDE);
        prefs.remove(preferencePrefix + Utils.PREF_GAS_STATION_ADDRESS);
        prefs.apply();
    }

    private static void deleteNearGasStation(Context context) {
        deleteGasStationPref(context, Utils.PREF_NEAR_GAS_STATION_PREFIX);
    }

    private static void deleteFavoriteGasStation(Context context) {
        deleteGasStationPref(context, Utils.PREF_FAVORITE_GAS_STATION_PREFIX);
    }

    private static GasStation getNearGasStations(Context context, Location lastKnownLocation, boolean bSearchWiderArea) {
        if (lastKnownLocation == null) return null; //just in case...

        if (bSearchWiderArea) {
            mSearchAreaRadius += Utils.MAP_DEFAULT_SEARCH_RADIUS;
        } else {
            mSearchAreaRadius = Utils.MAP_DEFAULT_SEARCH_RADIUS;
        }

        Retrofit.Builder retrofitBuilder = new Retrofit.Builder()
                .baseUrl(ClientConfig.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create());
        Retrofit retrofit = retrofitBuilder.build();

        RetrofitClient client = retrofit.create(RetrofitClient.class);

        Map<String, String> params = new HashMap<>();
        params.put(ClientConfig.paramLocation,
                ClientConfig.formatParamLocation(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()));
        params.put(ClientConfig.paramRadius, String.valueOf(mSearchAreaRadius)); // in meters
        params.put(ClientConfig.paramType, ClientConfig.paramTypeValue); // only Gas Stations
        params.put(ClientConfig.paramKey,
                context.getString(R.string.google_api_key)); // Google Maps API key

        Call<GasStationsList> gasStationsCall =  client.getStations(params);
        try {
            Response<GasStationsList> response = gasStationsCall.execute();
            if (response.code() == 200) {
                GasStationsList gasStationsList = response.body();

                if (gasStationsList != null) {

                    List<Result> gasStationListResult = gasStationsList.getResults();

                    List<GasStation> gasStationList = new ArrayList<>();

                    if (gasStationListResult.size() > 0) {
                        for (Result gasStation : gasStationListResult) {
                            Double gasLat = gasStation.getGeometry().getLocation().getLat();
                            Double gasLon = gasStation.getGeometry().getLocation().getLng();

                            gasStationList.add(new GasStation(
                                    gasStation.getPlaceId(),
                                    gasStation.getName(),
                                    gasStation.getIcon(),
                                    gasLat,
                                    gasLon,
                                    gasStation.getVicinity(),
                                    gasStation));

                        }

                        // get the nearest Gas Station
                        GasStation nearGasStation = null;
                        for (GasStation gasStation:gasStationList) {
                            if (nearGasStation == null) {
                                nearGasStation = gasStation;
                            }
                            else{
                                if (nearGasStation.getDistanceTo(lastKnownLocation) > gasStation.getDistanceTo(lastKnownLocation)) {
                                    nearGasStation = gasStation;
                                }
                            }
                        }
                        return nearGasStation;
                    }
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static GasStation getFavoriteGasStationFromLocalDB(Context context, Location lastKnownLocation){
        ContentResolver cr = context.getContentResolver();
        Cursor cursor = cr.query(
                GasStationContract.GasStationEntry.CONTENT_URI, null, null, null, null);
        ArrayList<GasStation> mAsyncGasStationList = new ArrayList<>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                GasStation gasStation = new GasStation(
                        cursor.getString(cursor.getColumnIndex(GasStationContract.GasStationEntry.COLUMN_GAS_STATION_ID)),
                        cursor.getString(cursor.getColumnIndex(GasStationContract.GasStationEntry.COLUMN_GAS_STATION_NAME)),
                        cursor.getString(cursor.getColumnIndex(GasStationContract.GasStationEntry.COLUMN_GAS_STATION_IMAGE_URL)),
                        cursor.getDouble(cursor.getColumnIndex(GasStationContract.GasStationEntry.COLUMN_GAS_STATION_LATITUDE)),
                        cursor.getDouble(cursor.getColumnIndex(GasStationContract.GasStationEntry.COLUMN_GAS_STATION_LONGITUDE)),
                        cursor.getString(cursor.getColumnIndex(GasStationContract.GasStationEntry.COLUMN_GAS_STATION_ADDRESS)),
                        GasStationTypeConverter.stringToGasStationList(
                                cursor.getString(cursor.getColumnIndex(GasStationContract.GasStationEntry.COLUMN_GAS_STATION_DETAILS))
                        )
                );
                mAsyncGasStationList.add(gasStation);
            }
            cursor.close();
        }
        // get the nearest Gas Station
        GasStation favoriteGasStation = null;
        for (GasStation gasStation:mAsyncGasStationList) {
            if (favoriteGasStation == null) {
                favoriteGasStation = gasStation;
            }
            else{
                if (favoriteGasStation.getDistanceTo(lastKnownLocation) > gasStation.getDistanceTo(lastKnownLocation)) {
                    favoriteGasStation = gasStation;
                }
            }
        }
        return favoriteGasStation;
    }

    private static void sendUpdateInfoToWidget(Context context) {
        Intent intent = new Intent(context, WmgWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(new ComponentName(context, WmgWidget.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        context.sendBroadcast(intent);
    }

    public static void forceUpdate(Context context) {
        new UpdateTask(
                new WeakReference<>(context)
        ).execute();
    }
}
