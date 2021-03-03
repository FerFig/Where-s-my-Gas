package com.ferfig.wheresmygas.ui;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.ferfig.wheresmygas.R;
import com.ferfig.wheresmygas.Utils;
import com.ferfig.wheresmygas.api.ClientConfig;
import com.ferfig.wheresmygas.api.RetrofitClient;
import com.ferfig.wheresmygas.job.SyncUtils;
import com.ferfig.wheresmygas.model.GasStation;
import com.ferfig.wheresmygas.model.GasStationState;
import com.ferfig.wheresmygas.model.maps.GasStationsList;
import com.ferfig.wheresmygas.model.maps.Result;
import com.ferfig.wheresmygas.provider.DbUtils;
import com.ferfig.wheresmygas.provider.GasStationsAsyncLoader;
import com.ferfig.wheresmygas.ui.adapter.GasStationsAdapter;
import com.ferfig.wheresmygas.ui.settings.SettingOption;
import com.ferfig.wheresmygas.ui.settings.SettingsActivity;
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
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

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
        , SwipeRefreshLayout.OnRefreshListener
        , SharedPreferences.OnSharedPreferenceChangeListener{

    private static final int REQUEST_ACTIVATION_RESULT = 27;
    private static final int REQUEST_PICK_LOCATION_RESULT = 63;
    private GoogleMap mMap;
    private CameraPosition mCameraPosition;

    private final List<Marker> mMarkers = new ArrayList<>();

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

    int mShowInfoWindow = SettingOption.SHOW_INFO_WINDOW.getValue();
    int mDisplayUnits = SettingOption.UNITS_METRIC.getValue();

    private String mSelectedGasStation;

    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.app_bar)
    Toolbar mAppBar;

    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.swipe_refresh_layout)
    SwipeRefreshLayout mSrlNearbyPlaces;

    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.tvSwipeRefreshMsg)
    TextView mTvSwipeRefreshMsg;

    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.fabActionSelectLocation)
    FloatingActionButton mFabActionSelectLocation;

    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.rvNearbyPlaces)
    RecyclerView mRvNearbyPlaces;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean isSwipeMsgVisible = false;
        if (savedInstanceState != null) {
            mLastKnowDeviceLocation = savedInstanceState.getParcelable(Utils.STORE_LAST_KNOW_LOCATION);
            mSearchAreaRadius = savedInstanceState.getLong(Utils.STORE_SEARCH_AREA_RADIUS);
            mSearchLocation = savedInstanceState.getParcelable(Utils.STORE_MAP_CAMERA_LOCATION);
            mCameraPosition = savedInstanceState.getParcelable(Utils.STORE_MAP_CAMERA_POSITION);
            mGasStationsList = savedInstanceState.getParcelable(Utils.STORE_GAS_STATIONS);
            mLastPickedLocation = savedInstanceState.getParcelable(Utils.STORE_LAST_PICKED_LOCATION);
            mFavoriteGasStations = savedInstanceState.getParcelableArrayList(Utils.STORE_FAVORITE_GAS_STATIONS);
            mSelectedGasStation = savedInstanceState.getString(Utils.STORE_SELECTED_GAS_STATION);
            isSwipeMsgVisible = savedInstanceState.getBoolean(Utils.STORE_SWIPE_MSG_VISIBILITY, false);
        }

        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        setSupportActionBar(mAppBar);
        mAppBar.setNavigationIcon(R.mipmap.ic_launcher);

        mSrlNearbyPlaces.setOnRefreshListener(this);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        readSettingsPreferences(sharedPreferences);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        // Construct a FusedLocationProviderClient to get Last Know Location
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Build the map.
        SupportMapFragment fragmentMap = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragmentMap);
        fragmentMap.getMapAsync(this);

        if (savedInstanceState == null) {
            // Retrieve favorite stations
            getDataFromLocalDB();
        }
        else{
            if (isSwipeMsgVisible) {
                mTvSwipeRefreshMsg.setVisibility(View.VISIBLE);
            }
        }

        // Schedule the firebase job service to update widget data
        SyncUtils.scheduleUpdateService(this);

        //Prompt to rate the app
        try {
            RateMe.promptToRate(this);
        } catch (final Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);

    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(Utils.STORE_LAST_KNOW_LOCATION, mLastKnowDeviceLocation);
            outState.putParcelable(Utils.STORE_MAP_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(Utils.STORE_MAP_CAMERA_LOCATION, mSearchLocation);
            outState.putLong(Utils.STORE_SEARCH_AREA_RADIUS, mSearchAreaRadius);
            outState.putParcelable(Utils.STORE_GAS_STATIONS, mGasStationsList);
            outState.putParcelable(Utils.STORE_LAST_PICKED_LOCATION, mLastPickedLocation);
            outState.putParcelableArrayList(Utils.STORE_FAVORITE_GAS_STATIONS, mFavoriteGasStations);
            outState.putString(Utils.STORE_SELECTED_GAS_STATION, mSelectedGasStation);
            outState.putBoolean(Utils.STORE_SWIPE_MSG_VISIBILITY,
                    mTvSwipeRefreshMsg.getVisibility()==View.VISIBLE);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.app_menu, menu);
        if (mShowInfoWindow==SettingOption.HIDE_INFO_WINDOW.getValue()){
            MenuItem infoItem = menu.findItem(R.id.menu_info);
            if (infoItem != null) {
                infoItem.setVisible(false);
            }
        }
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.menu_info:
                showInfoWindow();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showInfoWindow() {
        final Dialog infoWindow = new Dialog(this);
        infoWindow.setContentView(R.layout.info_window);
        Window dialogWindow = infoWindow.getWindow();
        if (dialogWindow != null) {
            // need to set this here to see the rounded shape without the white background
            dialogWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        ConstraintLayout infoWindowLayout = infoWindow.findViewById(R.id.info_window);
        infoWindowLayout.setOnClickListener(f -> infoWindow.dismiss());
        infoWindow.show();
    }

    @Override
    public void onPerformSnackBarAction(SnackBarActions action) {
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
                updateRefreshingUi();
                selectLocation();
                break;
            case REQUEST_LOCATION_ACTIVATION:
                updateRefreshingUi();
                startActivityForResult(
                        new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS),
                        REQUEST_ACTIVATION_RESULT);
        }
    }

    @Override
    public void onRefresh() {
        if (Utils.noInternetIsAvailable(this)) {
            Utils.showSnackBar(findViewById(android.R.id.content),
                    getString(R.string.sb_text_no_internet_connectivity),
                    getString(R.string.snackbar_action_retry),
                    Snackbar.LENGTH_INDEFINITE,
                    MainActivity.this,
                    SnackBarActions.RETRY_CONNECTION_REFRESH);
            return;
        }

        if (!Utils.userHasRefreshed(getApplicationContext())) {
            //only show refresh textview the first time
            Utils.setUserHasRefreshed(getApplicationContext());

            if (Utils.isDeviceInLandscape(this)) {
                mTvSwipeRefreshMsg.setVisibility(View.GONE);
            }
            else{
                mTvSwipeRefreshMsg.setVisibility(View.INVISIBLE);
            }
        }

        mIsRefreshing = true;
        mSrlNearbyPlaces.setRefreshing(true);

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
                                Snackbar.LENGTH_SHORT);
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
                            Snackbar.LENGTH_SHORT);
                }
                break;
        }
    }

    private void getNearbyGasStations(boolean bSearchWiderArea){
        if (Utils.noInternetIsAvailable(this)) {
            updateRefreshingUi();

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
            // ... from system service ...
            mSearchLocation = Utils.getLastKnownLocation(this);

            if (mSearchLocation == null){
                // .. or from shared preferences
                mSearchLocation = SyncUtils.getLastLocationFromPreferences(getApplicationContext());
            }

            // ...if we still cant get the last location prompt the user to select one
            if (mSearchLocation == null){
                updateRefreshingUi();

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
                ClientConfig.formatParamLocation(mSearchLocation.getLatitude(), mSearchLocation.getLongitude()));
        params.put(ClientConfig.paramRadius, String.valueOf(mSearchAreaRadius)); // in meters
        params.put(ClientConfig.paramType, ClientConfig.paramTypeValue); // only Gas Stations
        params.put(ClientConfig.paramKey, getString(R.string.google_api_key)); // Google Maps API key

        Call<GasStationsList> gasStationsCall =  client.getStations(params);
        gasStationsCall.enqueue(new Callback<GasStationsList>() {
            @Override
            public void onResponse(@NonNull Call<GasStationsList> call, @NonNull Response<GasStationsList> response) {
                updateRefreshingUi();

                if (response.code() == 200) {
                    GasStationsList gasStationsList = response.body();

                    if (gasStationsList != null) {

                        List<Result> gasStationListResult = gasStationsList.getResults();

                        if (gasStationListResult.size() > 0) {

                            mGasStationsList = gasStationsList;
                            addStationsToMap(mGasStationsList.getResults(), true);

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
                updateRefreshingUi();
                Utils.showSnackBar(findViewById(android.R.id.content),
                        getString(R.string.sb_text_error_failed_network_request),
                        getString(R.string.snackbar_action_retry),
                        Snackbar.LENGTH_INDEFINITE,
                        MainActivity.this,
                        SnackBarActions.RETRY_GET_NEARBY_STATIONS);
            }
        });
    }

    private void updateRefreshingUi() {
        if (mIsRefreshing) {
            mIsRefreshing = false;
            mSrlNearbyPlaces.setRefreshing(false);
        }
    }

    private void addStationsToMap(List<Result> gasStationListResult, boolean bFocusStations) {
        mMap.clear(); //remove any previous added marker...
        mMarkers.clear();

        LatLngBounds.Builder markersBounds = new LatLngBounds.Builder(); //store markers bounds

        //TODO: is mLastKnowDeviceLocation is null -- avoid crash
        //Also add the current location bounds so it doesn't disappear from the map
        markersBounds.include(new LatLng(mLastKnowDeviceLocation.getLatitude(),
                mLastKnowDeviceLocation.getLongitude()));

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
            gasStationImageUrl = gasStation.getIcon();

            LatLng location = new LatLng(gasLat, gasLon);

            MarkerOptions markerOptions = new MarkerOptions()
                    .title(gasStation.getName())
                    .position(location)
                    .snippet(Utils.formatDistance(this, distance));

            markersBounds.include(markerOptions.getPosition());

            Marker newMarker = mMap.addMarker(markerOptions);
            newMarker.setTag(gasStation);

            updateMarkerUi(newMarker);

            mMarkers.add(newMarker);

            GasStation newGasStation = new GasStation(
                    gasStation.getPlaceId(),
                    gasStation.getName(),
                    gasStationImageUrl,
                    gasLat,
                    gasLon,
                    gasStation.getVicinity(),
                    gasStation);

            newGasStation.isSelected = newGasStation.getPlaceId().equals(mSelectedGasStation);

            gasStationList.add(newGasStation);
        }

        // when from savedInstanceState keep the camera at that position
        if (bFocusStations) {
            LatLngBounds allBounds = markersBounds.build();
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(allBounds, 80);
            //mMap.moveCamera(cameraUpdate);
            mMap.animateCamera(cameraUpdate);
        }

        addStationsToRecyclerView(gasStationList);
    }

    @SuppressLint("WrongConstant")
    private void addStationsToRecyclerView(List<GasStation> gasStationList) {
        GasStationsAdapter mainGasStationsAdapter = new GasStationsAdapter(this,
                gasStationList,
                mFavoriteGasStations,
                mLastKnowDeviceLocation,
                gasStationData -> selectGasStation(gasStationData, gasStationList),
                gasStationData -> {
                    if (Utils.isFavoriteGasStation(gasStationData.getPlaceId(), mFavoriteGasStations)) {
                        removeFromFavorites(gasStationData);
                    } else {
                        addToFavorites(gasStationData);
                    }
                    // refresh UI
                    refreshFavoritesUi();

                    // also update widget info because changes have been made in favorite Gas Stations
                    SyncUtils.forceUpdate(getApplicationContext());
                },
                gasStationData -> {
                    if (gasStationData.getState() == GasStationState.CLOSED) {
                        Utils.alertBuilder(MainActivity.this, getString(R.string.closedQuestionText), null,
                                getString(R.string.Yes),
                                (dialog, which) -> startDirectionsToGasStation(gasStationData),
                                null,
                                null,
                                getString(R.string.No) ,
                                null
                        );
                    }else{
                        startDirectionsToGasStation(gasStationData);
                    }
                }
        );

        mRvNearbyPlaces.setLayoutManager(new LinearLayoutManager(
                this,
                OrientationHelper.VERTICAL,
                false));

        mRvNearbyPlaces.setAdapter(mainGasStationsAdapter);
    }

    private void startDirectionsToGasStation(GasStation gasStationData) {
        Intent mDirectionsIntent = Utils.buildDirectionsToIntent(gasStationData,
                true); // try to open turn by turn in google maps -- for Google credits ;)
        if (mDirectionsIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mDirectionsIntent);
        } else { // try to open turn by turn in other app if possible...
            mDirectionsIntent = Utils.buildDirectionsToIntent(gasStationData, false);
            if (mDirectionsIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mDirectionsIntent);
            } else {
                Utils.showSnackBar(findViewById(android.R.id.content),
                        getString(R.string.sb_text_navigation_is_not_possible),
                        Snackbar.LENGTH_LONG);
            }
        }
    }

    private void selectGasStation(GasStation gasStationData, List<GasStation> gasStationList) {
        for (GasStation gs : gasStationList) {
            gs.isSelected = false;
        }
        for (Marker marker : mMarkers) {
            Result gasStation = (Result)marker.getTag();
            if (gasStation != null){
                if (gasStation.getPlaceId().equals(gasStationData.getPlaceId())) {
                    gasStationData.isSelected = true;
                    mSelectedGasStation = gasStation.getPlaceId();
                    marker.showInfoWindow();
                    mMap.animateCamera(CameraUpdateFactory.newLatLng(
                            marker.getPosition()));
                    break;
                }
            }
        }
        mRvNearbyPlaces.getAdapter().notifyDataSetChanged();
    }

    private void refreshFavoritesUi() {
        // refresh markers on map
        for (Marker marker:mMarkers) {
            updateMarkerUi(marker);
        }
        // force refresh of recycler view layout
        mRvNearbyPlaces.getAdapter().notifyDataSetChanged();
    }

    private void updateMarkerUi(Marker marker) {
        Result gasStation = (Result) marker.getTag();
        if (gasStation != null) {
            if (gasStation.getOpeningHours() != null) {
                if (gasStation.getOpeningHours().getOpenNow()) {
                    // open now
                    marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                } else {
                    // not opened = show in different color
                    marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                }
            } else { // unknow if it's open or not! show dimmed/different color...
                marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
            }
        }
    }

    /**
     * Manipulates the map when it's available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        updateRefreshingUi();

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
        if (requestCode == Utils.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionGranted = true;
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
            if (mGasStationsList!=null && mGasStationsList.getResults() !=null && mGasStationsList.getResults().size()>0) {
                addStationsToMap(mGasStationsList.getResults(), false);
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

                SyncUtils.invalidateLastKnownLocation(getApplicationContext());
            }

            // Set the OnMyLocationButtonClickListener to center the map back in last know location
            mMap.setOnMyLocationButtonClickListener(() -> {
                if (mLastKnowDeviceLocation != null) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(mLastKnowDeviceLocation.getLatitude(), mLastKnowDeviceLocation.getLongitude()),
                            Utils.MAP_DEFAULT_ZOOM));
                }
                return false;
            });

            // Set the OnCameraIdleListener to retrieve gas stations based in map new position,
            // either by dragging the map or selecting location from pick location activity
            mMap.setOnCameraIdleListener(() -> {
                if (mSearchLocation != null && mMap.getCameraPosition() != null && mMap.getCameraPosition().target != null) {
                    Location currentLocation = new Location(LocationManager.GPS_PROVIDER);
                    currentLocation.setLatitude(mMap.getCameraPosition().target.latitude);
                    currentLocation.setLongitude(mMap.getCameraPosition().target.longitude);
                    if (currentLocation.distanceTo(mSearchLocation) > (mSearchAreaRadius == 0 ? Utils.MAP_DEFAULT_SEARCH_RADIUS : mSearchAreaRadius)) {
                        mSearchLocation = currentLocation;

                        if (!Utils.userHasRefreshed(getApplicationContext())) {
                            mTvSwipeRefreshMsg.setVisibility(View.VISIBLE);
                        }
                    }
                }
            });

            mMap.setOnMarkerClickListener(marker -> {
                Result gasStationData = (Result) marker.getTag();
                if (gasStationData != null) {
                    // make sure that the corresponding item is visible in recycler view
                    LinearLayoutManager linearLayoutManager = (LinearLayoutManager) mRvNearbyPlaces.getLayoutManager();
                    GasStationsAdapter gasStationAdapter = (GasStationsAdapter) mRvNearbyPlaces.getAdapter();
                    List<GasStation> gasStationInAdapter = gasStationAdapter.getData();
                    int position = 0;
                    for (GasStation adapterGasStation : gasStationInAdapter) {
                        if (adapterGasStation.getPlaceId().equals(gasStationData.getPlaceId())) {
//                                RecyclerView.SmoothScroller smoothScroller = new LinearSmoothScroller(getApplicationContext()) {
//                                    @Override protected int getVerticalSnapPreference() {
//                                        return LinearSmoothScroller.SNAP_TO_START;
//                                    }
//                                };
//                                smoothScroller.setTargetPosition(position);
//                                linearLayoutManager.startSmoothScroll(smoothScroller);

                            selectGasStation(adapterGasStation, gasStationInAdapter);

                            linearLayoutManager.smoothScrollToPosition(mRvNearbyPlaces, null, position);
                            break;
                        }
                        position++;
                    }
                }
                return false;
            });
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
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
                locationResult.addOnCompleteListener(this, task -> {
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

                        SyncUtils.saveLastLocationToPreferences(getApplicationContext(), mLastKnowDeviceLocation);

                        getNearbyGasStations(false);
                    }
                });
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    @OnClick({R.id.fabActionSelectLocation})
    public void fabClick(View view) {
        selectLocation();
    }

    private void selectLocation() {
        if (Utils.hasLocationPermission(this)) {
            boolean activityStarted = false;
            try {
                //TODO: replace placebuilder with new API
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
                            Snackbar.LENGTH_LONG);
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
        }
    }

    private void removeFromFavorites(GasStation gasStation) {
        // delete favorites from DB using the content provider
        if (DbUtils.deleteGasStationFromDB(getApplicationContext(), gasStation)) {

            for (int i = 0; i < mFavoriteGasStations.size(); i++) {
                if (mFavoriteGasStations.get(i).getPlaceId().equals(gasStation.getPlaceId())) {
                    mFavoriteGasStations.remove(i);
                    break;
                }
            }
        }
    }

    public void getDataFromLocalDB() {

        LoaderManager.LoaderCallbacks<ArrayList<GasStation>> callback = MainActivity.this;

        //TODO: replace deprecated API
        Loader<String> gasStationLoaderFromLocalDB = getSupportLoaderManager().getLoader(GasStationsAsyncLoader.LOADER_ID);
        if (gasStationLoaderFromLocalDB!=null){
            getSupportLoaderManager().restartLoader(GasStationsAsyncLoader.LOADER_ID, null, callback);
        }else {
            getSupportLoaderManager().initLoader(GasStationsAsyncLoader.LOADER_ID, null, callback);
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

    /* Preferences */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        readSharedPrefs(sharedPreferences, key);
    }

    private void readSettingsPreferences(SharedPreferences sharedPreferences) {
        readSharedPrefs(sharedPreferences, getString(R.string.pref_show_info_window));
        readSharedPrefs(sharedPreferences, getString(R.string.pref_units));
    }

    private void readSharedPrefs(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_show_info_window))){
            int currentVal = Integer.parseInt(
                    sharedPreferences.getString(key, String.valueOf(SettingOption.SHOW_INFO_WINDOW.getValue()))
            );
            if (mShowInfoWindow != currentVal){
                //if it has changed...
                mShowInfoWindow = currentVal;

                // this will trigger the menu creation method again
                invalidateOptionsMenu();
            }
            else{
                Log.i(Utils.TAG, "MainActivity: ... from settings no changes");
            }
        }
        if (key.equals(getString(R.string.pref_units))){
            int currentVal = Integer.parseInt(
                    sharedPreferences.getString(key, String.valueOf(SettingOption.UNITS_METRIC.getValue()))
            );
            if (mDisplayUnits != currentVal){
                //if it has changed...
                mDisplayUnits = currentVal;

                refreshUiMeasures();
            }
            else{
                Log.i(Utils.TAG, "MainActivity: ... from settings no changes");
            }
        }
    }

    private void refreshUiMeasures() {
        updateMarkerData();

        if (mRvNearbyPlaces != null && mRvNearbyPlaces.getAdapter() != null) {
            // force refresh of recycler view layout
            mRvNearbyPlaces.getAdapter().notifyDataSetChanged();
        }
    }

    private void updateMarkerData() {
        for (Marker marker:mMarkers) {
            Result gasStation = (Result) marker.getTag();
            if (gasStation != null) {
                Location gasLocation = new Location(LocationManager.GPS_PROVIDER);
                gasLocation.setLatitude(gasStation.getGeometry().getLocation().getLat());
                gasLocation.setLongitude(gasStation.getGeometry().getLocation().getLng());
                float distance = gasLocation.distanceTo(mLastKnowDeviceLocation); // meters
                marker.setSnippet(Utils.formatDistance(this, distance));
                if (marker.isInfoWindowShown()){
                    // this will refresh the info window distance :)
                    marker.hideInfoWindow();
                    marker.showInfoWindow();
                }
            }
        }
    }
}
