package com.udacity.ferfig.wheresmygas.ui.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import com.udacity.ferfig.wheresmygas.R;
import com.udacity.ferfig.wheresmygas.Utils;
import com.udacity.ferfig.wheresmygas.model.GasStation;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link WmgWidgetConfigureActivity WmgWidgetConfigureActivity}
 */
public class WmgWidget extends AppWidgetProvider {

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        int widgetSelectedOption = WmgWidgetConfigureActivity.getWidgetPref(context, appWidgetId);

        // TODO: read near and favorite(near) gas station from sharedPreferences
        GasStation nearGasStationData = new GasStation(null, "BP",null,null,null,
                520f,null,null);
        GasStation favoriteGasStationData = new GasStation(null, "Galp",null,null,null,
                1230f,null,null);

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.wmg_widget);
        views.setTextViewText(R.id.tvWidgetNearGasStationName, nearGasStationData.getName());
        views.setTextViewText(R.id.tvWidgetNearGasStationDistance, Utils.formatDistance(nearGasStationData.getDistance()));
        views.setTextViewText(R.id.tvWidgetFavoriteGasStationName, favoriteGasStationData.getName());
        views.setTextViewText(R.id.tvWidgetFavoriteGasStationDistance, Utils.formatDistance(favoriteGasStationData.getDistance()));

        if(widgetSelectedOption == SelectedOption.NEAR.getValue()) {
            views.setViewVisibility(R.id.widgetFavoriteView, View.GONE);
        } else if(widgetSelectedOption == SelectedOption.FAVORITE.getValue()) {
            views.setViewVisibility(R.id.widgetNearView, View.GONE);
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

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
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
            WmgWidgetConfigureActivity.deleteRecipePref(context, appWidgetId);
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

