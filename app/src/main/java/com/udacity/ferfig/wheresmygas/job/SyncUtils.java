package com.udacity.ferfig.wheresmygas.job;

import android.content.Context;
import android.content.SharedPreferences;
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
import com.udacity.ferfig.wheresmygas.Utils;
import com.udacity.ferfig.wheresmygas.model.GasStation;

import java.util.concurrent.TimeUnit;

public class SyncUtils {

    private static final String JOB_IDENTITY = "wmg_sync_job";

    private static boolean sInitialized;

    private static final int JOB_INTERVAL_MINUTES = 15;
    private static final int JOB_INTERVAL_SECONDS = (int) TimeUnit.MINUTES.toSeconds(JOB_INTERVAL_MINUTES);
    private static final int JOB_FLEXTIME_SECONDS = JOB_INTERVAL_SECONDS;

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
        prefs.putString(preferencePrefix + Utils.PREF_GAS_STATION_ID, gasStation.getId());
        prefs.putString(preferencePrefix + Utils.PREF_GAS_STATION_NAME, gasStation.getName());
        prefs.putString(preferencePrefix + Utils.PREF_GAS_STATION_IMAGE_URL, gasStation.getImageUrl());
        prefs.putLong(preferencePrefix + Utils.PREF_GAS_STATION_LATITUDE,
                Double.doubleToRawLongBits(gasStation.getLatitude()));
        prefs.putLong(preferencePrefix + Utils.PREF_GAS_STATION_LONGITUDE,
                Double.doubleToRawLongBits(gasStation.getLongitude()));
        prefs.putString(preferencePrefix + Utils.PREF_GAS_STATION_ADDRESS, gasStation.getAddress());
        prefs.apply();
    }

    public static void saveNearGasStationToPreferences(Context context, GasStation gasStation) {
        saveGasStationPref(context, gasStation, Utils.PREF_NEAR_GAS_STATION_PREFIX);
    }

    public static void saveFavoriteGasStationToPreferences(Context context, GasStation gasStation) {
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
}
