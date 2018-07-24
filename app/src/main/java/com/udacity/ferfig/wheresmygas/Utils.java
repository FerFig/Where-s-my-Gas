package com.udacity.ferfig.wheresmygas;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

import java.text.DecimalFormat;

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

    public static final int MAP_DEFAULT_ZOOM = 15;
    private static final LatLng MAP_DEFAULT_LOCATION = new LatLng(38.736946, -9.142685); //Portugal - Lisbon location

    public static final long MAP_DEFAULT_SEARCH_RADIUS = 1500;

    public static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

    public static boolean isInternetAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo ni = null;
        if (cm != null) {
            ni = cm.getActiveNetworkInfo();
        }
        return ni != null && ni.isConnected();
    }

    public static boolean isLocationServiceActive(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    public static void showSnackBar(View container, String message, String actionText, int duration,
                                    final SnackBarAction mCallback, final SnackBarActions action) {
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
        TextView textView = sbView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextColor(Color.YELLOW);
        snackbar.show();
    }

    public static String formatDistance(float distance) {
        DecimalFormat decimalFormat = new DecimalFormat("#.00");
        StringBuilder stringBuilder = new StringBuilder();
        if (distance<1000) {
            stringBuilder.append(decimalFormat.format(distance));
            stringBuilder.append("m");
        }else {
            stringBuilder.append(decimalFormat.format(distance/1000));
            stringBuilder.append("km");
        }
        return stringBuilder.toString();
    }

    public enum SnackBarActions {
        RETRY_CONNECTION,
        REQUEST_PERMISSIONS,
        RETRY_GET_NEARBY_STATIONS,
        RETRY_GET_NEARBY_STATIONS_IN_WIDER_AREA,
        REQUEST_LOCATION_ACTIVATION
    }

    public static boolean hasLocationPermission(Context context) {
        /*
         * Check location permission, so that we can get the location of the
         * device.
         */
        return ContextCompat.checkSelfPermission(context,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestLocationPermission(Activity activity) {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        ActivityCompat.requestPermissions(activity,
                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                Utils.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
    }

    public static Location getLastKnownLocation(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        Location location = null;
        if (locationManager != null) {
            try {
                Criteria criteria = new Criteria();
                location = locationManager.getLastKnownLocation(
                        locationManager.getBestProvider(criteria, false));
            }catch (SecurityException e){
                Log.d(TAG, "Security exception");
            }
        }
        if (location == null) {
            location = new Location(LocationManager.GPS_PROVIDER);
            location.setLatitude(Utils.MAP_DEFAULT_LOCATION.latitude);
            location.setLongitude(Utils.MAP_DEFAULT_LOCATION.longitude);
        }
        return location;
    }
}
