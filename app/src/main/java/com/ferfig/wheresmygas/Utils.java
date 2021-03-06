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
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.ferfig.wheresmygas.model.GasStation;
import com.ferfig.wheresmygas.ui.SnackBarAction;
import com.ferfig.wheresmygas.ui.SnackBarActions;
import com.ferfig.wheresmygas.ui.settings.SettingOption;
import com.google.android.material.snackbar.Snackbar;

import java.text.DecimalFormat;
import java.util.List;

import static android.content.Context.CONNECTIVITY_SERVICE;

public class Utils {

    private Utils() {}

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

    public static final int MAP_DEFAULT_ZOOM = 15;

    public static final long MAP_DEFAULT_SEARCH_RADIUS = 2500;

    public static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network nw = cm.getActiveNetwork();
            if (nw == null) return false;
            NetworkCapabilities actNw = cm.getNetworkCapabilities(nw);
            return actNw == null || !(actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH));
        } else {
            NetworkInfo nwInfo = cm.getActiveNetworkInfo();
            return nwInfo == null || !nwInfo.isConnected();
        }
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
                .setAction(actionText, view -> mCallback.onPerformSnackBarAction(action));

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
        String unitPreference =
                sharedPreferences.getString(context.getString(R.string.pref_units),
                        SettingOption.UNITS_METRIC);

        if (unitPreference.equals(SettingOption.UNITS_IMPERIAL)) {
            float miles = distance * 0.000621371192f;
            float yards = miles * 1760;
            if ( yards  < 1000 ){
                decimalFormat = new DecimalFormat("0 ");
                stringBuilder.append(decimalFormat.format(yards));
                stringBuilder.append("yd");
            }
            else {
                decimalFormat = new DecimalFormat("0.00 ");
                stringBuilder.append(decimalFormat.format(miles));
                stringBuilder.append("mi");
            }
        }
        else { // Metric
            if (distance < 1000) {
                decimalFormat = new DecimalFormat("0 ");
                stringBuilder.append(decimalFormat.format(distance));
                stringBuilder.append("m");
            } else {
                decimalFormat = new DecimalFormat("0.00 ");
                stringBuilder.append(decimalFormat.format(distance / 1000));
                stringBuilder.append("km");
            }
        }
        return stringBuilder.toString();
    }

    public static Intent buildDirectionsToIntent(@NonNull GasStation gasStationData, boolean withGoogleMaps ) {
        // Prepare the intent to call navigation to Gas Station

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
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
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

    public static boolean isFavoriteGasStation(@NonNull String gasStationId, @NonNull List<GasStation> favoritesGasStations) {
        for (GasStation gasStation:favoritesGasStations) {
            if (gasStation.getPlaceId().equals(gasStationId)){
                return true;
            }
        }
        return false;
    }

    public static boolean isDeviceInLandscape(@NonNull Context context){
        return context.getResources().getBoolean(R.bool.isInLandscape);
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
        if (title != null && title.length() > 0) {
            bld.setTitle(title);
        }else {
            bld.setTitle(R.string.app_name);
        }
        if (okBtnText != null && okBtnText.length() > 0) {
            bld.setPositiveButton(okBtnText,  okBtnClickListener);
        }
        if (neutralBtnText != null && neutralBtnText.length() > 0) {
            bld.setNeutralButton(neutralBtnText,  neutralBtnClickListener);
        }
        if (cancelBtnText != null && cancelBtnText.length() > 0) {
            bld.setNegativeButton(cancelBtnText,  cancelBtnClickListener);
        }
        bld.create().show();
    }

    public static boolean readBooleanSetting(@NonNull Context context, @NonNull String prefName, boolean defaultValue) {
        boolean mSetting;
        try {
            SharedPreferences mSettings = PreferenceManager.getDefaultSharedPreferences(context);
            mSetting = mSettings.getBoolean(prefName, defaultValue);
        } catch (Exception e) {
            mSetting = defaultValue;
        }
        return mSetting;
    }

    @SuppressWarnings({"unused", "RedundantSuppression"})
    public static String readStringSetting(@NonNull Context context, @NonNull String prefName) {
        return readStringSetting(context, prefName, "");
    }

    public static String readStringSetting(@NonNull Context context, @NonNull String prefName, String defaultValue) {
        try {
            SharedPreferences mSettings = PreferenceManager.getDefaultSharedPreferences(context);
            defaultValue = mSettings.getString(prefName, defaultValue);
        } catch (Exception e) {
            Log.d(TAG, "readStringSetting: " + e.getMessage());
        }
        return defaultValue;
    }

    public static long readLongSetting(@NonNull Context context, @NonNull String prefName) {
        return readLongSetting(context, prefName, 0);
    }

    public static long readLongSetting(@NonNull Context context, @NonNull String prefName, long defaultValue) {
        long mSetting;
        try {
            SharedPreferences mSettings = PreferenceManager.getDefaultSharedPreferences(context);
            mSetting = mSettings.getLong(prefName, defaultValue);
        } catch (Exception e) {
            mSetting = defaultValue;
        }
        return mSetting;
    }

    public static void saveSetting(@NonNull Context context, @NonNull String prefName, boolean prefValue) {
        SharedPreferences mSettings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putBoolean(prefName, prefValue);
        editor.apply();
    }


    @SuppressWarnings({"unused", "RedundantSuppression"})
    public static void saveSetting(@NonNull Context context, @NonNull String prefName, String prefValue) {
        SharedPreferences mSettings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putString(prefName, prefValue);
        editor.apply();
    }

    public static void saveSetting(@NonNull Context context, @NonNull String prefName, long prefValue) {
        SharedPreferences mSettings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putLong(prefName, prefValue);
        editor.apply();
    }

    public static boolean isDarkModeActive(Context context) {
        String darkPref = context.getString(R.string.SystemDarkModeIsActive);
        Log.w(TAG, "isDarkModeEnabled: darkPref = " + darkPref );
        return (darkPref.equals("Yes"));
    }
}
