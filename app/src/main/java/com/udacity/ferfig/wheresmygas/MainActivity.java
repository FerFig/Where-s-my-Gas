package com.udacity.ferfig.wheresmygas;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.udacity.ferfig.wheresmygas.Api.ClientConfig;
import com.udacity.ferfig.wheresmygas.Api.RetrofitClient;
import com.udacity.ferfig.wheresmygas.Utils.SnackBarActions;
import com.udacity.ferfig.wheresmygas.model.GasStation;
import com.udacity.ferfig.wheresmygas.model.maps.GasStationsList;
import com.udacity.ferfig.wheresmygas.model.maps.Result;
import com.udacity.ferfig.wheresmygas.provider.DbUtils;
import com.udacity.ferfig.wheresmygas.provider.GasStationsAsyncLoader;
import com.udacity.ferfig.wheresmygas.ui.GasStationsAdapter;

import java.util.ArrayList;
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
        implements OnMapReadyCallback, SnackBarAction, LoaderManager.LoaderCallbacks<ArrayList<GasStation>>
        , SwipeRefreshLayout.OnRefreshListener{

    private static final int REQUEST_ACTIVATION_RESULT = 27;
    private static final int REQUEST_PICK_LOCATION_RESULT = 63;
    private GoogleMap mMap;
    private CameraPosition mCameraPosition;

    private List<Marker> mMarkers = new ArrayList<>();

    private boolean mLocationPermissionGranted;

    // The entry point to the Fused Location Provider, to retrieve last known location
    private FusedLocationProviderClient mFusedLocationProviderClient;

    private Location mLastKnowDeviceLocation;
    private Location mSearchLocation;

    private long mSearchAreaRadius = Utils.MAP_DEFAULT_SEARCH_RADIUS;

    private LatLngBounds mLastPickedLocation;

    private GasStationsList mGasStationsList;

    private ArrayList<GasStation> mFavoriteGasStations;

    private boolean mIsRefreshing = false;

    @BindView(R.id.swipe_refresh_layout)
    SwipeRefreshLayout mSrlNearbyPlaces;

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
            mFavoriteGasStations = savedInstanceState.getParcelableArrayList(Utils.STORE_FAVORITE_GAS_STATIONS);
        }

        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        mSrlNearbyPlaces.setOnRefreshListener(this);

        // Construct a FusedLocationProviderClient to get Last Know Location
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Build the map.
        SupportMapFragment fragmentMap = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragmentMap);
        fragmentMap.getMapAsync(this);

        if (savedInstanceState == null) {
            mFabActionSelectLocation.setTag(Utils.FAB_STATE_PICK_LOCATION);

            // Retrieve favorite stations
            getDataFromLocalDB();
        }
        else{
            mFabActionSelectLocation.setTag(savedInstanceState.getString(Utils.STORE_FAB_STATE));
        }
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
            outState.putParcelableArrayList(Utils.STORE_FAVORITE_GAS_STATIONS, mFavoriteGasStations);
            outState.putString(Utils.STORE_FAB_STATE, String.valueOf(mFabActionSelectLocation.getTag()));
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPerformSnackBarAction(SnackBarActions action) {
        //TODO: implement all snackbar actions
        switch (action) {
            case RETRY_CONNECTION:
                onMapReady(mMap);
                break;
            case RETRY_CONNECTION_REFRESH:
                onRefresh();
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
            case SELECT_LOCATION:
                UpdateRefreshingUi();
                selectLocation();
                break;
            case REQUEST_LOCATION_ACTIVATION:
                UpdateRefreshingUi();
                startActivityForResult(
                        new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS),
                        REQUEST_ACTIVATION_RESULT);
        }
    }

    @Override
    public void onRefresh() {
        if (Utils.noInternetIsAvailable(getApplicationContext())) {
            Utils.showSnackBar(findViewById(android.R.id.content),
                    getString(R.string.sb_text_no_internet_connectivity),
                    getString(R.string.snackbar_action_retry),
                    Snackbar.LENGTH_INDEFINITE,
                    MainActivity.this,
                    SnackBarActions.RETRY_CONNECTION_REFRESH);
            return;
        }

        mIsRefreshing = true;
        mSrlNearbyPlaces.setRefreshing(mIsRefreshing);

        getNearbyGasStations(false);
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
        if (Utils.noInternetIsAvailable(getApplicationContext())) {
            UpdateRefreshingUi();

            Utils.showSnackBar(findViewById(android.R.id.content),
                    getString(R.string.sb_text_no_internet_connectivity),
                    getString(R.string.snackbar_action_retry),
                    Snackbar.LENGTH_INDEFINITE,
                    MainActivity.this,
                    SnackBarActions.RETRY_CONNECTION);
            return;
        }

        // If search location is null try to get the last known location...
        if (mSearchLocation == null){
            mSearchLocation = Utils.getLastKnownLocation(this);

            // ...if we still cant get the last location prompt the user to select one
            if (mSearchLocation == null){
                UpdateRefreshingUi();

                Utils.showSnackBar(findViewById(android.R.id.content),
                        getString(R.string.sb_text_unknown_location),
                        getString(R.string.snackbar_action_select_location),
                        Snackbar.LENGTH_INDEFINITE,
                        MainActivity.this,
                        SnackBarActions.SELECT_LOCATION);
                return;
            }
        }

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
                UpdateRefreshingUi();

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
                UpdateRefreshingUi();

                Utils.showSnackBar(findViewById(android.R.id.content),
                        getString(R.string.sb_text_error_failed_network_request),
                        getString(R.string.snackbar_action_retry),
                        Snackbar.LENGTH_INDEFINITE,
                        MainActivity.this,
                        SnackBarActions.RETRY_GET_NEARBY_STATIONS);
            }
        });
    }

    private void UpdateRefreshingUi() {
        if (mIsRefreshing) {
            mIsRefreshing = false;
            mSrlNearbyPlaces.setRefreshing(false);
        }
    }

    private void addStationsToMap(List<Result> gasStationListResult) {
        mMap.clear(); //remove any previous added marker...
        mMarkers.clear();

        LatLngBounds.Builder markersBounds = new LatLngBounds.Builder(); //store markers bounds

        if (mSearchLocation == mLastKnowDeviceLocation) {
            //Also add the current location bounds so it doesn't disappear from the map
            markersBounds.include(new LatLng(mLastKnowDeviceLocation.getLatitude(),
                    mLastKnowDeviceLocation.getLongitude()));
        }

        // Store GasStations list to show in recycle view adapter
        List<GasStation> gasStationList = new ArrayList<>();

        for (Result gasStation : gasStationListResult) {
            float distance;
            Double gasLat = gasStation.getGeometry().getLocation().getLat();
            Double gasLon = gasStation.getGeometry().getLocation().getLng();

            Location gasLocation = new Location(LocationManager.GPS_PROVIDER);
            gasLocation.setLatitude(gasLat);
            gasLocation.setLongitude(gasLon);
            distance = gasLocation.distanceTo(mLastKnowDeviceLocation); // meters

            String gasStationImageUrl;
//TODO add gas station images... if time permits
//            List<Photo> gasStationPhotos = gasStation.getPhotos();
//            if (gasStationPhotos.size()>0){
//                gasStationImageUrl = ClientConfig.BASE_URL.concat(
//                        "/maps/api/place/photo?maxwidth=200&photoreference=").concat(
//                        gasStationPhotos.get(0).getPhotoReference()).concat("&key=")
//                        .concat(getString(R.string.google_api_key));
//            }else {
                gasStationImageUrl = gasStation.getIcon();
//            }

            LatLng location = new LatLng(gasLat, gasLon);

            MarkerOptions markerOptions = new MarkerOptions()
                    .title(gasStation.getName())
                    .position(location)
                    .snippet(Utils.formatDistance(distance));

            if (Utils.isFavoriteGasStation(gasStation.getId(), mFavoriteGasStations)) {
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
            }
            else { // non favorite
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
            }

            markersBounds.include(markerOptions.getPosition());

            Marker newMarker = mMap.addMarker(markerOptions);
            newMarker.setTag(gasStation.getId());
            mMarkers.add(newMarker);

            gasStationList.add(new GasStation(
                    gasStation.getId(),
                    gasStation.getName(),
                    gasStationImageUrl,
                    gasLat,
                    gasLon,
                    distance,
                    gasStation.getVicinity(),
                    gasStation));
        }

        LatLngBounds allBounds = markersBounds.build();
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(allBounds, 60); //TODO: add (padding) to preferences?!
        //mMap.moveCamera(cameraUpdate);
        mMap.animateCamera(cameraUpdate);

        addStationsToRecyclerView(gasStationList);

        if (mFabActionSelectLocation.getTag().equals(Utils.FAB_STATE_REFRESH)) {
            mFabActionSelectLocation.setImageResource(R.drawable.ic_place_24dp);
            mFabActionSelectLocation.setTag(Utils.FAB_STATE_PICK_LOCATION);
        }
    }

    private void addStationsToRecyclerView(List<GasStation> gasStationList) {
//        mProgressBar.setVisibility(View.GONE);
//        mErrorMessage.setVisibility(View.GONE);
//        mMainRecyclerView.setVisibility(View.VISIBLE);

        GasStationsAdapter mainGasStationsAdapter = new GasStationsAdapter(this,
                gasStationList,
                mFavoriteGasStations,
                new GasStationsAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(GasStation gasStationData) {
                        for (Marker marker:mMarkers) {
                            if (marker.getTag() == gasStationData.getId()) {
                                marker.showInfoWindow();
                                mMap.animateCamera(CameraUpdateFactory.newLatLng(
                                        marker.getPosition()));
                                break;
                            }
                        }

                    }
                },
                new GasStationsAdapter.OnFavoritesClickListener() {
                    @Override
                    public void onFavoritesClick(GasStation gasStationData) {
                        if (Utils.isFavoriteGasStation(gasStationData.getId(), mFavoriteGasStations)) {
                            removeFromFavorites(gasStationData);
                        } else {
                            addToFavorites(gasStationData);
                        }
                    }
                },
                new GasStationsAdapter.OnDirectionsClickListener() {
                    @Override
                    public void onDirectionsClick(GasStation gasStationData) {
                        Intent mDirectionsIntent = Utils.buildDirectionsToIntent(gasStationData);
                        if (mDirectionsIntent.resolveActivity(getPackageManager()) != null) {
                            startActivity(mDirectionsIntent);
                        }
                    }
                }
        );

        mRvNearbyPlaces.setLayoutManager(new GridLayoutManager(
                this,
                1,
                OrientationHelper.VERTICAL,
                false));

        mRvNearbyPlaces.setAdapter(mainGasStationsAdapter);
    }

    /**
     * Manipulates the map when it's available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        UpdateRefreshingUi();

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
            if (Utils.noInternetIsAvailable(this)) {
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
                        //getNearbyGasStations(false);
                    }
                }
            });

            mMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
                @Override
                public void onCameraMoveStarted(int i) {
                    if (mFabActionSelectLocation.getTag().equals(Utils.FAB_STATE_PICK_LOCATION)) {
                        mFabActionSelectLocation.setImageResource(R.drawable.ic_sync_24dp);
                        mFabActionSelectLocation.setTag(Utils.FAB_STATE_REFRESH);
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
        if (mFabActionSelectLocation.getTag().equals(Utils.FAB_STATE_PICK_LOCATION)) {
            selectLocation();
        }
        else {
            getNearbyGasStations(false);
        }
    }

    private void selectLocation() {
        if (Utils.hasLocationPermission(this)) {
            boolean activityStarted = false;
            try {
                PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
                Intent intent = builder.build(this);

                if (mLastPickedLocation != null) builder.setLatLngBounds(mLastPickedLocation);

                startActivityForResult(intent, REQUEST_PICK_LOCATION_RESULT);
                activityStarted = true;
            } catch (GooglePlayServicesRepairableException e) {
                Log.e(Utils.TAG, String.format("GooglePlayServices Inconsistent State [%s]", e.getMessage()));
            } catch (GooglePlayServicesNotAvailableException e) {
                Log.e(Utils.TAG, String.format("GooglePlayServices Not Available [%s]", e.getMessage()));
            } catch (Exception e) {
                Log.e(Utils.TAG, String.format("PlacePicker Exception: %s", e.getMessage()));
            } finally {
                if (!activityStarted){
                    Utils.showSnackBar(findViewById(android.R.id.content),
                            getString(R.string.sb_text_google_play_services_error),
                            null,
                            Snackbar.LENGTH_LONG,
                            null,
                            null);
                }
            }
        } else {
            Utils.showSnackBar(findViewById(android.R.id.content),
                    getString(R.string.sb_text_retry_permissions_message),
                    getString(R.string.snackbar_action_permissions_request),
                    Snackbar.LENGTH_INDEFINITE,
                    MainActivity.this,
                    SnackBarActions.REQUEST_PERMISSIONS);
        }
    }

    private void addToFavorites(GasStation gasStation){
        // save favorites to DB using the content provider
        if (DbUtils.addGasStationToDB(getApplicationContext(), gasStation)){
            mFavoriteGasStations.add(gasStation);

            // TODO: refresh ui
        }
    }

    private void removeFromFavorites(GasStation gasStation) {
        // delete favorites from DB using the content provider
        if (DbUtils.deleteGasStationFromDB(getApplicationContext(), gasStation)) {

            for (int i = 0; i < mFavoriteGasStations.size(); i++) {
                if (mFavoriteGasStations.get(i).getId().equals(gasStation.getId())) {
                    mFavoriteGasStations.remove(i);
                    break;
                }
            }

            // TODO: refresh ui
        }
    }

    private void getDataFromLocalDB() {

        LoaderManager.LoaderCallbacks<ArrayList<GasStation>> callback = MainActivity.this;

        Bundle bundleForLoader = new Bundle();

        Loader<String> gasStationLoaderFromLocalDB = getSupportLoaderManager().getLoader(GasStationsAsyncLoader.LOADER_ID);
        if (gasStationLoaderFromLocalDB!=null){
            getSupportLoaderManager().restartLoader(GasStationsAsyncLoader.LOADER_ID, bundleForLoader, callback);
        }else {
            getSupportLoaderManager().initLoader(GasStationsAsyncLoader.LOADER_ID, bundleForLoader, callback);
        }
    }

    /* BEGIN LoaderCallbacks Methods */
    @NonNull
    @Override
    public Loader<ArrayList<GasStation>> onCreateLoader(int id, @Nullable Bundle args) {
        return new GasStationsAsyncLoader(this, getContentResolver());
    }

    @Override
    public void onLoadFinished(@NonNull Loader<ArrayList<GasStation>> loader, ArrayList<GasStation> data) {
        //loader finished, discard it
        getSupportLoaderManager().destroyLoader(loader.getId());

        if (null != data) {
            mFavoriteGasStations = data;
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<ArrayList<GasStation>> loader) {

    }
    /* END LoaderCallbacks Methods */
}
