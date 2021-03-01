package com.ferfig.wheresmygas;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.ferfig.wheresmygas.model.GasStation;
import com.ferfig.wheresmygas.ui.SnackBarAction;
import com.ferfig.wheresmygas.ui.SnackBarActions;
import com.ferfig.wheresmygas.ui.settings.SettingOption;
import com.google.android.gms.common.util.Strings;
import com.google.android.material.snackbar.Snackbar;

import java.text.DecimalFormat;
import java.util.ArrayList;

import static android.content.Context.CONNECTIVITY_SERVICE;

public class Utils {

    public static final String TAG = "Where's my Gas";

    // Keys to store activity state
    public static final String STORE_LAST_KNOW_LOCATION = "wmg_last_know_location";
    public static final String STORE_SEARCH_AREA_RADIUS = "wmg_search_radius";
    public static final String STORE_MAP_CAMERA_POSITION = "wmg_cam_position";
    public static final String STORE_MAP_CAMERA_LOCATION = "wmg_cam_location";
    public static final String STORE_GAS_STATIONS = "wmg_gas_stations";
    public static final String STORE_LAST_PICKED_LOCATION = "wmg_last_picked_location";
    public static final String STORE_FAVORITE_GAS_STATIONS = "wmg_favorites";
    public static final String STORE_SELECTED_GAS_STATION = "wmg_selected_gas_station";
    public static final String STORE_SWIPE_MSG_VISIBILITY = "wmg_swipe_feedback";

    public static final int MAP_DEFAULT_ZOOM = 15;

    public static final long MAP_DEFAULT_SEARCH_RADIUS = 2500;

    public static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

    private static final String USER_SWIPED_REFRESH = "wsg_user_refreshed";

    // Shared Preferences
    public static final String PREFS_NAME = "com.ferfig.wheresmygas.preferences";
    public static final String PREF_NEAR_GAS_STATION_PREFIX = "com.ferfig.wmg.near_";
    public static final String PREF_FAVORITE_GAS_STATION_PREFIX = "com.ferfig.wmg.fav_";
    public static final String PREF_GAS_STATION_ID = "id";
    public static final String PREF_GAS_STATION_NAME = "name";
    public static final String PREF_GAS_STATION_IMAGE_URL = "image.url";
    public static final String PREF_GAS_STATION_LATITUDE = "latitude";
    public static final String PREF_GAS_STATION_LONGITUDE = "longitude";
    public static final String PREF_GAS_STATION_ADDRESS = "address";
    public static final String PREF_LAST_KNOWN_LATITUDE = "com.ferfig.wmg.last.latitude";
    public static final String PREF_LAST_KNOWN_LONGITUDE = "com.ferfig.wmg.last.longitude";

    public static boolean noInternetIsAvailable(@NonNull Activity activity) {
        ConnectivityManager cm = (ConnectivityManager) activity.getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo ni = null;
        if (cm != null) {
            ni = cm.getActiveNetworkInfo();
        }
        return (ni == null || !ni.isConnected());
    }

    public static boolean isLocationServiceActive(@NonNull Activity activity) {
        LocationManager locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null && (
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                        || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        );
    }

    public static void showSnackBar(@NonNull View container, @NonNull String message, int duration) {
        Snackbar snackbar = Snackbar
                .make(container, message, duration);

        snackbar.setActionTextColor(Color.RED);

        View sbView = snackbar.getView();
        TextView textView = sbView.findViewById(R.id.snackbar_text);
        textView.setTextColor(Color.YELLOW);
        snackbar.show();
    }

    public static void showSnackBar(@NonNull View container, @NonNull String message, @NonNull String actionText, int duration,
                                    @NonNull final SnackBarAction mCallback, final SnackBarActions action) {
        Snackbar snackbar = Snackbar
                .make(container, message, duration)
                .setAction(actionText, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mCallback.onPerformSnackBarAction(action);
                    }
                });

        snackbar.setActionTextColor(Color.RED);

        View sbView = snackbar.getView();
        TextView textView = sbView.findViewById(R.id.snackbar_text);
        textView.setTextColor(Color.YELLOW);
        snackbar.show();
    }

    public static String formatDistance(@NonNull Context context, float distance) {
        DecimalFormat decimalFormat;
        StringBuilder stringBuilder = new StringBuilder();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        int unitPreference = Integer.parseInt(
                sharedPreferences.getString(context.getString(R.string.pref_units),
                        String.valueOf(SettingOption.UNITS_METRIC.getValue())));

        if (unitPreference == SettingOption.UNITS_IMPERIAL.getValue()) {
            float miles = distance * 0.000621371192f;
            decimalFormat = new DecimalFormat("0.00");
            stringBuilder.append(decimalFormat.format(miles));
            stringBuilder.append("mi");
        }
        else { // Metric
            if (distance < 1000) {
                decimalFormat = new DecimalFormat("0");
                stringBuilder.append(decimalFormat.format(distance));
                stringBuilder.append("m");
            } else {
                decimalFormat = new DecimalFormat("0.0");
                stringBuilder.append(decimalFormat.format(distance / 1000));
                stringBuilder.append("km");
            }
        }
        return stringBuilder.toString();
    }

    public static Intent buildDirectionsToIntent(@NonNull GasStation gasStationData, boolean withGoogleMaps ) {
        // Prepare the intent to call navigation to Gas Station

//        // With Maps URLs universal cross-platform
//        final String BASE_GOOGLE_DIRECTIONS_URL = "https://www.google.com/maps/dir/?api=1";
//        Uri directionsUri = Uri.parse(BASE_GOOGLE_DIRECTIONS_URL)
//                .buildUpon()
//                .appendQueryParameter("travelmode", "driving")
//                .appendQueryParameter("destination_place_id", gasStationData.getId())
//                .appendQueryParameter("destination", // required parameter
//                        String.valueOf(gasStationData.getLatitude())
//                                .concat(",")
//                                .concat(String.valueOf(gasStationData.getLongitude())))
//                .appendQueryParameter("dir_action", "navigate") // start navigation immediately
//                .build();

        // With Google Maps Intents for Android
        final String BASE_GOOGLE_NAVIGATION_URL = "google.navigation:q=%1$s,%2$s&mode=d";
        Uri directionsUri = Uri.parse(
                String.format(BASE_GOOGLE_NAVIGATION_URL,
                        gasStationData.getLatitude(),
                        gasStationData.getLongitude()));

        Intent directionsIntent = new Intent(Intent.ACTION_VIEW, directionsUri);
        if (withGoogleMaps) {
            directionsIntent.setPackage("com.google.android.apps.maps");
        }
        return directionsIntent;
    }

    public static boolean hasLocationPermission(@NonNull Context context) {
        /*
         * Check location permission, so that we can get the location of the
         * device.
         */
        return ContextCompat.checkSelfPermission(context,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestLocationPermission(@NonNull Activity activity) {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        ActivityCompat.requestPermissions(activity,
                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                Utils.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
    }

    public static Location getLastKnownLocation(@NonNull Context contextActivity) {
        LocationManager locationManager = (LocationManager) contextActivity.getSystemService(Context.LOCATION_SERVICE);
        Location location = null;
        if (locationManager != null) {
            try {
                Criteria criteria = new Criteria();
                location = locationManager.getLastKnownLocation(
                        locationManager.getBestProvider(criteria, true));
            }catch (SecurityException e){
                Log.d(TAG, "Security exception");
            }
        }
        return location;
    }

    public static boolean isFavoriteGasStation(@NonNull String gasStationId, @NonNull ArrayList<GasStation> favoritesGasStations) {
        if (favoritesGasStations != null){
            for (GasStation gasStation:favoritesGasStations) {
                if (gasStation.getPlaceId().equals(gasStationId)){
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isDeviceInLandscape(@NonNull Context context){
        return context.getResources().getBoolean(R.bool.isInLandscape);
    }

    public static boolean userHasRefreshed(@NonNull Context context) {
        boolean mSetting;
        try {
            SharedPreferences mSettings = PreferenceManager.getDefaultSharedPreferences(context);
            mSetting = mSettings.getBoolean(USER_SWIPED_REFRESH, false);
        } catch (Exception e) {
            mSetting = false;
        }
        return mSetting;
    }

    public static void setUserHasRefreshed(@NonNull Context context) {
        SharedPreferences mSettings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putBoolean(USER_SWIPED_REFRESH, true);
        editor.apply();
    }

    public static void alertBuilder(@NonNull Context context, @NonNull String message,
                                    String title,
                                    String okBtnText,
                                    DialogInterface.OnClickListener okBtnClickListener,
                                    String neutralBtnText,
                                    DialogInterface.OnClickListener neutralBtnClickListener,
                                    String cancelBtnText,
                                    DialogInterface.OnClickListener cancelBtnClickListener){
        final  AlertDialog.Builder bld = new AlertDialog.Builder(context);
        bld.setIcon(R.drawable.ic_launcher_foreground);
        bld.setMessage(message);
        if (!Strings.isEmptyOrWhitespace(title)) {
            bld.setTitle(title);
        }else {
            bld.setTitle(R.string.app_name);
        }
        if (!Strings.isEmptyOrWhitespace(okBtnText)) {
            bld.setPositiveButton(okBtnText,  okBtnClickListener);
        }
        if (!Strings.isEmptyOrWhitespace(neutralBtnText)) {
            bld.setNeutralButton(neutralBtnText,  neutralBtnClickListener);
        }
        if (!Strings.isEmptyOrWhitespace(cancelBtnText)) {
            bld.setNegativeButton(cancelBtnText,  cancelBtnClickListener);
        }
        bld.create().show();
    }

    public static boolean readBooleanSetting(@NonNull Context context, @NonNull String prefName, boolean defaultValue) {
        boolean mSetting=false;
        try {
            SharedPreferences mSettings = PreferenceManager.getDefaultSharedPreferences(context);
            mSetting = mSettings.getBoolean(prefName, defaultValue);
        } catch (Exception e) {
            mSetting = defaultValue;
        }
        return mSetting;
    };

    public static String readStringSetting(@NonNull Context context, @NonNull String prefName) {
        String mSetting="";
        try {
            SharedPreferences mSettings = PreferenceManager.getDefaultSharedPreferences(context);
            mSetting = mSettings.getString(prefName, mSetting);
        } catch (Exception e) {
            mSetting = "";
        }
        return mSetting;
    };

    public static long readLongSetting(@NonNull Context context, @NonNull String prefName) {
        long mSetting=0;
        try {
            SharedPreferences mSettings = PreferenceManager.getDefaultSharedPreferences(context);
            mSetting = mSettings.getLong(prefName, mSetting);
        } catch (Exception e) {
            mSetting = 0;
        }
        return mSetting;
    };

    public static void saveSetting(@NonNull Context context, @NonNull String prefName, boolean prefValue) {
        SharedPreferences mSettings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putBoolean(prefName, prefValue);
        editor.apply();
    };

    public static void saveSetting(@NonNull Context context, @NonNull String prefName, String prefValue) {
        SharedPreferences mSettings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putString(prefName, prefValue);
        editor.apply();
    };

    public static void saveSetting(@NonNull Context context, @NonNull String prefName, long prefValue) {
        SharedPreferences mSettings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putLong(prefName, prefValue);
        editor.apply();
    };
}
