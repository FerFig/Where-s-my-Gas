package com.udacity.ferfig.wheresmygas;

import android.content.Intent;
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

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
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
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback, SnackBarAction{

    private static final int REQUEST_ACTIVATION_RESULT = 27;
    private static final int REQUEST_PICK_LOCATION_RESULT = 63;
    private GoogleMap mMap;
    private CameraPosition mCameraPosition;

//    The entry points to the Places API.
//    private GeoDataClient mGeoDataClient;
//    private PlaceDetectionClient mPlaceDetectionClient;

    private boolean mLocationPermissionGranted;

    // The entry point to the Fused Location Provider, to retrieve last known location
    private FusedLocationProviderClient mFusedLocationProviderClient;

    private Location mLastKnowDeviceLocation;
    private Location mSearchLocation;

    private long mSearchAreaRadius = Utils.MAP_DEFAULT_SEARCH_RADIUS;

    private LatLngBounds mLastPickedLocation;

    private GasStationsList mGasStationsList;

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
            mLastKnowDeviceLocation = savedInstanceState.getParcelable(Utils.STORE_LAST_KNOW_LOCATION);
            mSearchAreaRadius = savedInstanceState.getLong(Utils.STORE_SEARCH_AREA_RADIUS);
            mSearchLocation = savedInstanceState.getParcelable(Utils.STORE_MAP_CAMERA_LOCATION);
            mCameraPosition = savedInstanceState.getParcelable(Utils.STORE_MAP_CAMERA_POSITION);
            mGasStationsList = savedInstanceState.getParcelable(Utils.STORE_GAS_STATIONS);
            mLastPickedLocation = savedInstanceState.getParcelable(Utils.STORE_LAST_PICKED_LOCATION);
        }

        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        // Construct a FusedLocationProviderClient to get Last Know Location
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Build the map.
        SupportMapFragment fragmentMap = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragmentMap);
        fragmentMap.getMapAsync(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(Utils.STORE_LAST_KNOW_LOCATION, mLastKnowDeviceLocation);
            outState.putParcelable(Utils.STORE_MAP_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(Utils.STORE_MAP_CAMERA_LOCATION, mSearchLocation);
            outState.putLong(Utils.STORE_SEARCH_AREA_RADIUS, mSearchAreaRadius);
            outState.putParcelable(Utils.STORE_GAS_STATIONS, mGasStationsList);
            outState.putParcelable(Utils.STORE_LAST_PICKED_LOCATION, mLastPickedLocation);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPerformSnackBarAction(SnackBarActions action) {
        //TODO: implement all snackbar actions
        switch (action) {
            case RETRY_CONNECTION:

                break;
            case REQUEST_PERMISSIONS:
                Utils.requestLocationPermission(this);
                break;
            case RETRY_GET_NEARBY_STATIONS:
                getNearbyGasStations(false);
                break;
            case RETRY_GET_NEARBY_STATIONS_IN_WIDER_AREA:
                getNearbyGasStations(true);
                break;
            case REQUEST_LOCATION_ACTIVATION:
                startActivityForResult(
                        new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS),
                        REQUEST_ACTIVATION_RESULT);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ACTIVATION_RESULT:
                onMapReady(mMap);
                break;
            case REQUEST_PICK_LOCATION_RESULT:
                if (resultCode == RESULT_OK){
                    Place place = PlacePicker.getPlace(this, data);
                    if (place == null) {
                        Utils.showSnackBar(findViewById(android.R.id.content),
                                getString(R.string.sb_text_no_place_selected),
                                null,
                                Snackbar.LENGTH_SHORT,
                                null,
                                null);
                    }
                    else {
                        mSearchLocation = new Location(LocationManager.GPS_PROVIDER);
                        mSearchLocation.setLatitude(place.getLatLng().latitude);
                        mSearchLocation.setLongitude(place.getLatLng().longitude);

                        mLastPickedLocation = place.getViewport();

                        getNearbyGasStations(false);
                    }
                }
                else{
                    Utils.showSnackBar(findViewById(android.R.id.content),
                            getString(R.string.sb_text_place_picker_cancelled),
                            null,
                            Snackbar.LENGTH_SHORT,
                            null,
                            null);
                }
                break;
        }
    }

    private void getNearbyGasStations(boolean bSearchWiderArea){
        if (bSearchWiderArea) {
            mSearchAreaRadius += Utils.MAP_DEFAULT_SEARCH_RADIUS; //TODO: add max search area logic
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
                ClientConfig.formatParamLocation(mSearchLocation.getLatitude(), mSearchLocation.getLongitude()));
        params.put(ClientConfig.paramRadius, String.valueOf(mSearchAreaRadius)); // in meters
        params.put(ClientConfig.paramType, ClientConfig.paramTypeValue); // only Gas Stations
        params.put(ClientConfig.paramKey, getString(R.string.google_api_key)); // Google Maps API key

        Call<GasStationsList> gasStationsCall =  client.getStations(params);
        gasStationsCall.enqueue(new Callback<GasStationsList>() {
            @Override
            public void onResponse(@NonNull Call<GasStationsList> call, @NonNull Response<GasStationsList> response) {
                if (response.code() == 200) {
                    GasStationsList gasStationsList = response.body();

                    if (gasStationsList != null) {
                        //TODO if needed and time permits... add status check?!
                        //String placesSearchStatus = gasStationsList.getStatus();
                        //if (placesSearchStatus.equals(RESPONSE_OK)){
                        //if (placesSearchStatus.equals(RESPONSE_ZERO_RESULTS)){
                        //if (placesSearchStatus.equals(RESPONSE_OVER_QUERY_LIMIT)){
                        //if (placesSearchStatus.equals(RESPONSE_REQUEST_DENIED)){
                        //if (placesSearchStatus.equals(RESPONSE_INVALID_REQUEST)){
                        //if (placesSearchStatus.equals(RESPONSE_UNKNOWN_ERROR)){

                        List<Result> gasStationListResult = gasStationsList.getResults();

                        if (gasStationListResult.size() > 0) {

                            mGasStationsList = gasStationsList;
                            addStationsToMap(mGasStationsList.getResults());

                        }
                        else {//gasStationsList.size()) == 0
                            //TODO: ask to redo search in wider area
                            Utils.showSnackBar(findViewById(android.R.id.content),
                                    getString(R.string.sb_text_response_empty_list),
                                    getString(R.string.snackbar_action_wider_search),
                                    Snackbar.LENGTH_INDEFINITE,
                                    MainActivity.this,
                                    SnackBarActions.RETRY_GET_NEARBY_STATIONS_IN_WIDER_AREA);
                        }
                    }
                    else { //gasStationsList == null
                        Utils.showSnackBar(findViewById(android.R.id.content),
                                getString(R.string.sb_text_response_null_message),
                                getString(R.string.snackbar_action_retry),
                                Snackbar.LENGTH_INDEFINITE,
                                MainActivity.this,
                                SnackBarActions.RETRY_GET_NEARBY_STATIONS);
                    }
                }
                else {
                    Utils.showSnackBar(findViewById(android.R.id.content),
                            getString(R.string.sb_text_response_failed_request),
                            getString(R.string.snackbar_action_retry),
                            Snackbar.LENGTH_INDEFINITE,
                            MainActivity.this,
                            SnackBarActions.RETRY_GET_NEARBY_STATIONS);
                }
            }

            @Override
            public void onFailure(@NonNull Call<GasStationsList> call, @NonNull Throwable t) {
                Utils.showSnackBar(findViewById(android.R.id.content),
                        getString(R.string.sb_text_error_failed_network_request),
                        getString(R.string.snackbar_action_retry),
                        Snackbar.LENGTH_INDEFINITE,
                        MainActivity.this,
                        SnackBarActions.RETRY_GET_NEARBY_STATIONS);
            }
        });
    }

    private void addStationsToMap(List<Result> gasStationListResult) {
        mMap.clear(); //remove any previous added marker...

        LatLngBounds.Builder markersBounds = new LatLngBounds.Builder(); //store markers bounds

        if (mSearchLocation == mLastKnowDeviceLocation) {
            //Also add the current location bounds so it doesn't disappear from the map
            markersBounds.include(new LatLng(mLastKnowDeviceLocation.getLatitude(),
                    mLastKnowDeviceLocation.getLongitude()));
        }

        for (Result gasStation : gasStationListResult) {
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
            if (gasStation.getName().equals("<favorite gas statio>")) {
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
            }
            if (gasStation.getOpeningHours() != null) {
                if (gasStation.getOpeningHours().getOpenNow()) {
                    // open now
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                } else {
                    // not opened = show in different color
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN));
                }
            } else { // unknow if it's open or not! show dimmed/different color...
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
            }

            markersBounds.include(markerOptions.getPosition());

            mMap.addMarker(markerOptions);
        }

        LatLngBounds allBounds = markersBounds.build();
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(allBounds, 60); //TODO: add (padding) to preferences?!
        //mMap.moveCamera(cameraUpdate);
        mMap.animateCamera(cameraUpdate);
//      setMainRecipAdapter(mRecipList);
    }

    /**
     * Manipulates the map when it's available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

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

        // Check if location is active on device...
        if (!Utils.isLocationServiceActive(this)) {
            Utils.showSnackBar(findViewById(android.R.id.content),
                    getString(R.string.sb_text_activate_location),
                    getString(R.string.snackbar_action_activate_location),
                    Snackbar.LENGTH_INDEFINITE,
                    MainActivity.this,
                    SnackBarActions.REQUEST_LOCATION_ACTIVATION);
        }
        else{
            // Check if internet is available...
            if (!Utils.isInternetAvailable(this)) {
                Utils.showSnackBar(findViewById(android.R.id.content),
                        getString(R.string.sb_text_no_internet_connectivity),
                        getString(R.string.snackbar_action_retry),
                        Snackbar.LENGTH_INDEFINITE,
                        MainActivity.this,
                        SnackBarActions.RETRY_CONNECTION);
            }
            else{
                // Check if permissions are granted...
                mLocationPermissionGranted = Utils.hasLocationPermission(this);

                if (!mLocationPermissionGranted) {
                    // Prompt to grant for permission.
                    Utils.requestLocationPermission(this);
                }
                else{
                    //Everything looks fine. App should now run properly! :D
                    resumeAppLoading();
                }
            }
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
            resumeAppLoading();
        }
        else {
            Utils.showSnackBar(findViewById(android.R.id.content),
                    getString(R.string.sb_text_retry_permissions_message),
                    getString(R.string.snackbar_action_permissions_request),
                    Snackbar.LENGTH_INDEFINITE,
                    MainActivity.this,
                    SnackBarActions.REQUEST_PERMISSIONS);
        }
    }

    private void resumeAppLoading(){
        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        if (mCameraPosition == null) {
            // Get the current location of the device and set the position of the map...
            // ... also retrieves nearby gas stations after current location
            getDeviceLocation();
        }
        else{
            //restoring from SavedInstanceState...

            //mMap.moveCamera(CameraUpdateFactory.newCameraPosition(mCameraPosition));
            if (mGasStationsList!=null && mGasStationsList.getResults() !=null && mGasStationsList.getResults().size()>0) {
                addStationsToMap(mGasStationsList.getResults());
            }
            else {
                getNearbyGasStations(false);
            }
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
            mMap.getUiSettings().setMapToolbarEnabled(false); //disable navigation controls in map layer
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnowDeviceLocation = null;
            }

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
                    if (currentLocation.distanceTo(mSearchLocation) > (mSearchAreaRadius ==0?Utils.MAP_DEFAULT_SEARCH_RADIUS: mSearchAreaRadius)) {
                        mSearchLocation = currentLocation;
                        getNearbyGasStations(false);
                    }
                }
            });
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
                            Log.e(Utils.TAG, "Exception: %s", task.getException());
                        }

                        if (mLastKnowDeviceLocation == null){
                            Log.d(Utils.TAG, "Current location is null. Using defaults.");

                            mLastKnowDeviceLocation = Utils.getLastKnownLocation(getApplicationContext());
                        }

                        if (mLastKnowDeviceLocation != null) {
                            // Move camera (immediately) here, so we don't see all the world map zooming in :)
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(mLastKnowDeviceLocation.getLatitude(), mLastKnowDeviceLocation.getLongitude()),
                                    Utils.MAP_DEFAULT_ZOOM));

                            mSearchLocation = mLastKnowDeviceLocation;
                            getNearbyGasStations(false);
                        }
                    }
                });
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    @OnClick({R.id.fabActionSelectLocation})
    public void fabClick(View view) {
        if (Utils.hasLocationPermission(this)) {
            try {
                PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
                Intent intent = builder.build(this);

                if (mLastPickedLocation != null) builder.setLatLngBounds(mLastPickedLocation);

                startActivityForResult(intent, REQUEST_PICK_LOCATION_RESULT);
            } catch (GooglePlayServicesRepairableException e) {
                Log.e(Utils.TAG, String.format("GooglePlayServices Inconsistent State [%s]", e.getMessage()));
            } catch (GooglePlayServicesNotAvailableException e) {
                Log.e(Utils.TAG, String.format("GooglePlayServices Not Available [%s]", e.getMessage()));
            } catch (Exception e) {
                Log.e(Utils.TAG, String.format("PlacePicker Exception: %s", e.getMessage()));
            }
        }
        else {
            Utils.showSnackBar(findViewById(android.R.id.content),
                    getString(R.string.sb_text_retry_permissions_message),
                    getString(R.string.snackbar_action_permissions_request),
                    Snackbar.LENGTH_INDEFINITE,
                    MainActivity.this,
                    SnackBarActions.REQUEST_PERMISSIONS);
        }
    }
}
