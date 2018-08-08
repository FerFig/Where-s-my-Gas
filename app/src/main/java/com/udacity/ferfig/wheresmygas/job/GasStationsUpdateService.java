package com.udacity.ferfig.wheresmygas.job;

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.os.AsyncTask;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.udacity.ferfig.wheresmygas.R;
import com.udacity.ferfig.wheresmygas.Utils;
import com.udacity.ferfig.wheresmygas.api.ClientConfig;
import com.udacity.ferfig.wheresmygas.api.RetrofitClient;
import com.udacity.ferfig.wheresmygas.model.GasStation;
import com.udacity.ferfig.wheresmygas.model.GasStationTypeConverter;
import com.udacity.ferfig.wheresmygas.model.maps.GasStationsList;
import com.udacity.ferfig.wheresmygas.model.maps.Result;
import com.udacity.ferfig.wheresmygas.provider.GasStationContract;
import com.udacity.ferfig.wheresmygas.ui.widget.WmgWidget;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class GasStationsUpdateService extends JobService {
    private AsyncTask mGasStationJobTask;

    private static Location mLastKnownLocation;
    private long mSearchAreaRadius = Utils.MAP_DEFAULT_SEARCH_RADIUS;

    @SuppressLint("StaticFieldLeak")
    @Override
    public boolean onStartJob(final JobParameters jobParams) {
        mGasStationJobTask = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {
                Context context = GasStationsUpdateService.this;

                // first we need the last know location to calculate current distances
                mLastKnownLocation = Utils.getLastKnownLocation(context);
                if (mLastKnownLocation == null) {
                    // try to get it from shared prefs
                    mLastKnownLocation = SyncUtils.getLastLocationFromPreferences(context);
                }else{
                    SyncUtils.saveLastLocationToPreferences(context, mLastKnownLocation);
                }
                if (mLastKnownLocation == null) return null;

                GasStation favoriteGasStation = getFavoriteGasStationFromLocalDB(context);
                if (favoriteGasStation != null){
                    SyncUtils.saveFavoriteGasStationToPreferences(context, favoriteGasStation);
                }
                else{
                    SyncUtils.deleteFavoriteGasStation(context);
                }

                GasStation nearGasStation = getNearGasStations(false);
                if (nearGasStation == null) { // try wider search...
                    for (int i = 0; i < 20; i++) { //...for 20 times
                        nearGasStation = getNearGasStations(true);
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

                sendUpdateInfoToWidget();
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                jobFinished(jobParams, false);
            }
        };
        mGasStationJobTask.execute();
        return true; // Is there still work going on? yep, in the async task...
    }

    @Override
    public boolean onStopJob(JobParameters jobParams) {
        // if the the job cant execute for some reason (no network...)
        if (mGasStationJobTask != null) {
            mGasStationJobTask.cancel(true);
        }
        return true; // Should this job be retried? sure
    }

    private GasStation getNearGasStations(boolean bSearchWiderArea) {
        if (mLastKnownLocation == null) return null; //just in case...

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
                ClientConfig.formatParamLocation(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude()));
        params.put(ClientConfig.paramRadius, String.valueOf(mSearchAreaRadius)); // in meters
        params.put(ClientConfig.paramType, ClientConfig.paramTypeValue); // only Gas Stations
        params.put(ClientConfig.paramKey, getString(R.string.google_api_key)); // Google Maps API key

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
                                    gasStation.getId(),
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
                                if (nearGasStation.getDistanceTo(mLastKnownLocation) > gasStation.getDistanceTo(mLastKnownLocation)) {
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

    private GasStation getFavoriteGasStationFromLocalDB(Context context){
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
                if (favoriteGasStation.getDistanceTo(mLastKnownLocation) > gasStation.getDistanceTo(mLastKnownLocation)) {
                    favoriteGasStation = gasStation;
                }
            }
        }
        return favoriteGasStation;
    }

    private void sendUpdateInfoToWidget() {
        Intent intent = new Intent(this, WmgWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(getApplication())
                .getAppWidgetIds(new ComponentName(getApplication(), WmgWidget.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);
    }
}
