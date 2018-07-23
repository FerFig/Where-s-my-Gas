package com.udacity.ferfig.wheresmygas;

import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.udacity.ferfig.wheresmygas.Api.ClientConfig;
import com.udacity.ferfig.wheresmygas.Api.RetrofitClient;
import com.udacity.ferfig.wheresmygas.Utils.SnackBarActions;
import com.udacity.ferfig.wheresmygas.model.GasStationsList;
import com.udacity.ferfig.wheresmygas.model.Result;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, SnackBarAction{

    private GoogleMap mMap;
//    private CameraPosition mCameraPosition;

//    The entry points to the Places API.
//    private GeoDataClient mGeoDataClient;
//    private PlaceDetectionClient mPlaceDetectionClient;

    private boolean mLocationPermissionGranted;

    // The entry point to the Fused Location Provider, to retrieve last known location
    private FusedLocationProviderClient mFusedLocationProviderClient;

    private Location mLastKnowDeviceLocation;
    private Location mSearchLocation;

    @BindView(R.id.svNearbyPlaces)
    ScrollView mSvNearbyPlaces;

    @BindView(R.id.fabActionSelectLocation)
    FloatingActionButton mFabActionSelectLocation;

    @BindView(R.id.rvNearbyPlaces)
    RecyclerView mRvNearbyPlaces;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mLastKnowDeviceLocation = savedInstanceState.getParcelable(Utils.STORE_MAP_CAMERA_LOCATION);
//            mCameraPosition = savedInstanceState.getParcelable(Utils.STORE_MAP_CAMERA_POSITION);
        }
        setContentView(R.layout.activity_main);
        // Construct a GeoDataClient.
//        mGeoDataClient = Places.getGeoDataClient(this);

        // Construct a PlaceDetectionClient.
//        mPlaceDetectionClient = Places.getPlaceDetectionClient(this);

        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Build the map.
        SupportMapFragment fragmentMap = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragmentMap);
        fragmentMap.getMapAsync(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(Utils.STORE_MAP_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(Utils.STORE_MAP_CAMERA_LOCATION, mLastKnowDeviceLocation);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPerformSnackBarAction(SnackBarActions action) {
        switch (action) {
            case RETRY_CONNECTION:
                //TODO: retry connection, do snack bar action
                break;
            case REQUEST_PERMISSIONS:
                Utils.requestLocationPermission(this);
                break;
            case RETRY_GET_NEARBY_STATIONS:
                break;
        }
    }

    private void getNearbyGasStations(){
        Retrofit.Builder retrofitBuilder = new Retrofit.Builder()
                .baseUrl(ClientConfig.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create());
        Retrofit retrofit = retrofitBuilder.build();

        RetrofitClient client = retrofit.create(RetrofitClient.class);

        Map<String, String> params = new HashMap<>();
        params.put(ClientConfig.paramLocation,
                ClientConfig.formatParamLocation(mSearchLocation.getLatitude(), mSearchLocation.getLongitude()));
        params.put(ClientConfig.paramRadius, Utils.MAP_DEFAULT_SEARCH_RADIUS); // in meters
        params.put(ClientConfig.paramType, ClientConfig.paramTypeValue); // only Gas Stations
        params.put(ClientConfig.paramKey, getString(R.string.google_api_key)); // Google Maps API key

        Call<GasStationsList> gasStationsCall =  client.getStations(params);
        gasStationsCall.enqueue(new Callback<GasStationsList>() {
            @Override
            public void onResponse(@NonNull Call<GasStationsList> call, @NonNull Response<GasStationsList> response) {
                if (response.code() == 200) {
                    GasStationsList gasStationsList = response.body();
                    mMap.clear(); //remove any previous added marker...

                    LatLngBounds.Builder markersBounds = new LatLngBounds.Builder(); //store markers bounds

                    if (mSearchLocation == mLastKnowDeviceLocation){
                        //Also add the current location bounds so it doesn't disappear from the map
                        markersBounds.include(new LatLng(mLastKnowDeviceLocation.getLatitude(),
                                mLastKnowDeviceLocation.getLongitude()));
                    }

                    for (Result gasStation: gasStationsList.getResults()) {
                        float distance;
                        Double gasLat = gasStation.getGeometry().getLocation().getLat();
                        Double gasLon = gasStation.getGeometry().getLocation().getLng();

                        Location gasLocation = new Location(LocationManager.GPS_PROVIDER);
                        gasLocation.setLatitude(gasLat);
                        gasLocation.setLongitude(gasLon);
                        distance = gasLocation.distanceTo(mLastKnowDeviceLocation); // meters

                        Log.d(Utils.TAG, "Gas Station: " + gasStation.getName() + " " + distance + "m");

                        LatLng location = new LatLng(gasLat, gasLon);

                        MarkerOptions markerOptions = new MarkerOptions()
                                .title(gasStation.getName())
                                .position(location)
                                .snippet(Utils.formatDistance(distance));

                        //TODO: favorite gas stations display...
                        if (gasStation.getName().equals("<favorite gas statio>")){
                            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
                        }
                        if ( gasStation.getOpeningHours() != null ) {
                            if (gasStation.getOpeningHours().getOpenNow()) {
                                // open now
                                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                            }else{
                                // not opened = show in different color
                                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN));
                            }
                        }
                        else{ // unknow if it's open or not! show dimmed/different color...
                            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                        }

                        markersBounds.include(markerOptions.getPosition());

                        mMap.addMarker(markerOptions);
                    }
                    LatLngBounds allBounds = markersBounds.build();
                    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(allBounds, 60); //TODO: add (padding) to preferences?!
                    //mMap.moveCamera(cameraUpdate);
                    mMap.animateCamera(cameraUpdate);
//                    setMainRecipAdapter(mRecipList);
                }
                else
                {
//                    tvErrorMessage.setText(R.string.error_failed_response);
                }
            }

            @Override
            public void onFailure(@NonNull Call<GasStationsList> call, @NonNull Throwable t) {
                //R.string.error_failed_network_request
                Utils.showSnackBar(findViewById(android.R.id.content),
                        getString(R.string.retry_internet_connection_message),
                        getString(R.string.retry_internet_connection),
                        Snackbar.LENGTH_INDEFINITE,
                        MainActivity.this,
                        SnackBarActions.RETRY_GET_NEARBY_STATIONS);

            }
        });
    }

    /**
     * Manipulates the map when it's available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.getUiSettings().setMapToolbarEnabled(false);

        // Set the OnMyLocationButtonClickListener to center the map back in last know location
        mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(mLastKnowDeviceLocation.getLatitude(), mLastKnowDeviceLocation.getLongitude()),
                        Utils.MAP_DEFAULT_ZOOM));
                return false;
            }
        });

        // Set the OnCameraIdleListener to retrieve gas stations based in map new position,
        // either by dragging the map or selecting location from pick location activity
        mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener(){
            @Override
            public void onCameraIdle() {
                Location currentLocation = new Location(LocationManager.GPS_PROVIDER);
                currentLocation.setLatitude(mMap.getCameraPosition().target.latitude);
                currentLocation.setLongitude(mMap.getCameraPosition().target.longitude);
                if (currentLocation.distanceTo(mSearchLocation) > Float.parseFloat(Utils.MAP_DEFAULT_SEARCH_RADIUS)) {
                    mSearchLocation = currentLocation;
                    getNearbyGasStations();
                }
            }
        });


//        // Use a custom info window adapter to handle multiple lines of text in the
//        // info window contents.
//        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
//
//            @Override
//            // Return null here, so that getInfoContents() is called next.
//            public View getInfoWindow(Marker arg0) {
//                return null;
//            }
//
//            @Override
//            public View getInfoContents(Marker marker) {
//                // Inflate the layouts for the info window, title and snippet.
//                View infoWindow = getLayoutInflater().inflate(R.layout.custom_info_contents,
//                        (FrameLayout) findViewById(R.id.fragmentMap), false);
//
//                TextView title = ((TextView) infoWindow.findViewById(R.id.title));
//                title.setText(marker.getTitle());
//
//                TextView snippet = ((TextView) infoWindow.findViewById(R.id.snippet));
//                snippet.setText(marker.getSnippet());
//
//                return infoWindow;
//            }
//        });

        mLocationPermissionGranted = Utils.hasLocationPermission(this);

        // Prompt the user for permission.
        if (!mLocationPermissionGranted) {
            Utils.requestLocationPermission(this);
        } else {
            // Turn on the My Location layer and the related control on the map.
            updateLocationUI();

            // Get the current location of the device and set the position of the map.
            getDeviceLocation();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case Utils.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
        if (mLocationPermissionGranted) {
            // Turn on the My Location layer and the related control on the map.
            updateLocationUI();

            // Get the current location of the device and set the position of the map.
            getDeviceLocation();
        }else
        {
            Utils.showSnackBar(findViewById(android.R.id.content),
                    getString(R.string.retry_permissions_message),
                    getString(R.string.retry_permissions),
                    Snackbar.LENGTH_INDEFINITE,
                    MainActivity.this,
                    SnackBarActions.REQUEST_PERMISSIONS);
        }

    }

    /**
     * Updates the map's UI settings based on whether the user has granted location permission.
     */
    private void updateLocationUI() {
        if (mMap == null) {
            //TODO: retry?!
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnowDeviceLocation = null;
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
            //TODO: snackbar retry?!
        }
    }

    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (mLocationPermissionGranted) {
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnowDeviceLocation = task.getResult();
                        } else {
                            Log.d(Utils.TAG, "Current location is null. Using defaults.");
                            Log.e(Utils.TAG, "Exception: %s", task.getException());

                            mLastKnowDeviceLocation = Utils.getLastKnownLocation(getApplicationContext());

                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }

                        // It's needed here, so we don't see all the world map zooming in :)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(mLastKnowDeviceLocation.getLatitude(), mLastKnowDeviceLocation.getLongitude()),
                                Utils.MAP_DEFAULT_ZOOM));

                        mSearchLocation = mLastKnowDeviceLocation;
                        getNearbyGasStations();
                    }
                });
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    @OnClick({R.id.fabActionSelectLocation})
    public void fabClick(View view) {
        Utils.showSnackBar(findViewById(android.R.id.content),
                "fab clicked!",
                "",
                Snackbar.LENGTH_SHORT,
                null,
                null);
    }
}
