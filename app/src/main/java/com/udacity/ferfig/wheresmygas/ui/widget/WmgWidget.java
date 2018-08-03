package com.udacity.ferfig.wheresmygas.ui.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.view.View;
import android.widget.RemoteViews;

import com.udacity.ferfig.wheresmygas.R;
import com.udacity.ferfig.wheresmygas.Utils;
import com.udacity.ferfig.wheresmygas.job.SyncUtils;
import com.udacity.ferfig.wheresmygas.model.GasStation;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link WmgWidgetConfigureActivity WmgWidgetConfigureActivity}
 */
public class WmgWidget extends AppWidgetProvider {

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        int widgetSelectedOption = WmgWidgetConfigureActivity.getWidgetPref(context, appWidgetId);

        Location lastKnownLocation = SyncUtils.getLastLocationFromPreferences(context);

        GasStation nearGasStationData = SyncUtils.getNearGasStationFromPreferences(context);
        boolean hasNearInfo = (!nearGasStationData.getId().isEmpty());

        GasStation favoriteGasStationData = SyncUtils.getFavoriteGasStationFromPreferences(context);
        boolean hasFavoriteInfo = (!favoriteGasStationData.getId().isEmpty());

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.wmg_widget);

        if (hasNearInfo) {
            views.setTextViewText(R.id.tvWidgetNearGasStationName, nearGasStationData.getName());
            if (lastKnownLocation == null){
                views.setTextViewText(R.id.tvWidgetNearGasStationDistance, ""); //unknown :(
            }else {
                views.setTextViewText(R.id.tvWidgetNearGasStationDistance,
                        Utils.formatDistance(nearGasStationData.getDistanceTo(lastKnownLocation)));
            }
            Intent mNearDirectionsIntent = Utils.buildDirectionsToIntent(nearGasStationData,
                    true); // try to open turn by turn in google maps -- for Google credits ;)
            if (mNearDirectionsIntent.resolveActivity(context.getPackageManager()) != null) {
                views.setOnClickPendingIntent(R.id.widgetNearView,
                        PendingIntent.getActivity(context, 0, mNearDirectionsIntent, 0));
            }
            else{
                // try to open turn by turn in other app if possible...
                mNearDirectionsIntent = Utils.buildDirectionsToIntent(nearGasStationData,
                        false);
                if (mNearDirectionsIntent.resolveActivity(context.getPackageManager()) != null) {
                    views.setOnClickPendingIntent(R.id.widgetNearView,
                            PendingIntent.getActivity(context, 0, mNearDirectionsIntent, 0));
                }
            }
        }

        if (hasFavoriteInfo) {
            views.setTextViewText(R.id.tvWidgetFavoriteGasStationName, favoriteGasStationData.getName());
            if (lastKnownLocation == null){
                views.setTextViewText(R.id.tvWidgetFavoriteGasStationDistance, ""); //unknown :(
            }
            else {
                views.setTextViewText(R.id.tvWidgetFavoriteGasStationDistance,
                        Utils.formatDistance(favoriteGasStationData.getDistanceTo(lastKnownLocation)));
            }
            Intent mFavoriteDirectionsIntent = Utils.buildDirectionsToIntent(favoriteGasStationData,
                    true); // try to open turn by turn in google maps -- for Google credits ;)
            if (mFavoriteDirectionsIntent.resolveActivity(context.getPackageManager()) != null){
                views.setOnClickPendingIntent(R.id.widgetFavoriteView,
                        PendingIntent.getActivity(context, 0, mFavoriteDirectionsIntent, 0));
            }
            else{
                // try to open turn by turn in other app if possible...
                mFavoriteDirectionsIntent = Utils.buildDirectionsToIntent(favoriteGasStationData,
                        false);
                if (mFavoriteDirectionsIntent.resolveActivity(context.getPackageManager()) != null) {
                    views.setOnClickPendingIntent(R.id.widgetFavoriteView,
                            PendingIntent.getActivity(context, 0, mFavoriteDirectionsIntent, 0));
                }
            }
        }

        refreshWidgetUi(widgetSelectedOption, hasNearInfo, hasFavoriteInfo, views);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private static void refreshWidgetUi(int widgetSelectedOption, boolean hasNearInfo, boolean hasFavoriteInfo, RemoteViews views) {
        // Refresh the UI
        if(widgetSelectedOption == SelectedOption.NEAR.getValue()) {
            views.setViewVisibility(R.id.widgetNearView, View.VISIBLE);
            views.setViewVisibility(R.id.widgetFavoriteView, View.GONE);
            if (hasNearInfo){
                views.setViewVisibility(R.id.widgetNearInfoMissing, View.GONE);
                views.setViewVisibility(R.id.tvWidgetNearGasStationName, View.VISIBLE);
                views.setViewVisibility(R.id.tvWidgetNearGasStationDistance, View.VISIBLE);
            }
            else{
                views.setViewVisibility(R.id.widgetNearInfoMissing, View.VISIBLE);
                views.setViewVisibility(R.id.tvWidgetNearGasStationName, View.GONE);
                views.setViewVisibility(R.id.tvWidgetNearGasStationDistance, View.GONE);
            }
        } else if(widgetSelectedOption == SelectedOption.FAVORITE.getValue()) {
            views.setViewVisibility(R.id.widgetNearView, View.GONE);
            views.setViewVisibility(R.id.widgetFavoriteView, View.VISIBLE);
            if (hasFavoriteInfo){
                views.setViewVisibility(R.id.widgetFavoriteInfoMissing, View.GONE);
                views.setViewVisibility(R.id.tvWidgetFavoriteGasStationName, View.VISIBLE);
                views.setViewVisibility(R.id.tvWidgetFavoriteGasStationDistance, View.VISIBLE);
            }
            else{
                views.setViewVisibility(R.id.widgetFavoriteInfoMissing, View.VISIBLE);
                views.setViewVisibility(R.id.tvWidgetFavoriteGasStationName, View.GONE);
                views.setViewVisibility(R.id.tvWidgetFavoriteGasStationDistance, View.GONE);
            }
        } else { // SelectedOption.BOTH
            views.setViewVisibility(R.id.widgetNearView, View.VISIBLE);
            views.setViewVisibility(R.id.widgetFavoriteView, View.VISIBLE);

            if (hasNearInfo){
                views.setViewVisibility(R.id.widgetNearInfoMissing, View.GONE);
                views.setViewVisibility(R.id.tvWidgetNearGasStationName, View.VISIBLE);
                views.setViewVisibility(R.id.tvWidgetNearGasStationDistance, View.VISIBLE);
            }
            else{
                views.setViewVisibility(R.id.widgetNearInfoMissing, View.VISIBLE);
                views.setViewVisibility(R.id.tvWidgetNearGasStationName, View.GONE);
                views.setViewVisibility(R.id.tvWidgetNearGasStationDistance, View.GONE);
            }
            if (hasFavoriteInfo){
                views.setViewVisibility(R.id.widgetFavoriteInfoMissing, View.GONE);
                views.setViewVisibility(R.id.tvWidgetFavoriteGasStationName, View.VISIBLE);
                views.setViewVisibility(R.id.tvWidgetFavoriteGasStationDistance, View.VISIBLE);
            }
            else{
                views.setViewVisibility(R.id.widgetFavoriteInfoMissing, View.VISIBLE);
                views.setViewVisibility(R.id.tvWidgetFavoriteGasStationName, View.GONE);
                views.setViewVisibility(R.id.tvWidgetFavoriteGasStationDistance, View.GONE);
            }
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
        for (int appWidgetId : appWidgetIds) {
            WmgWidgetConfigureActivity.deletePref(context, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

