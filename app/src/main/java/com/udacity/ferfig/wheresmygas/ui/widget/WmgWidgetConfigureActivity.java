package com.udacity.ferfig.wheresmygas.ui.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.udacity.ferfig.wheresmygas.R;
import com.udacity.ferfig.wheresmygas.Utils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * The configuration screen for the {@link WmgWidget WmgWidget} AppWidget.
 */
public class WmgWidgetConfigureActivity extends Activity {

    private static final String PREF_PREFIX_KEY = "wmg.widget_";
    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @BindView( R.id.widget_radio_group)
    RadioGroup mRadioGroup;

    @BindView( R.id.widget_selection_both)
    RadioButton mRbSelectBoth;

    @BindView( R.id.widget_selection_near_only)
    RadioButton mRbSelectNearOnly;

    @BindView( R.id.widget_selection_favorite_only)
    RadioButton mRbSelectFavoriteOnly;

    public WmgWidgetConfigureActivity() {
        super();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED);

        setContentView(R.layout.wmg_widget_configure);

        ButterKnife.bind(this);

        // Find the widget id from the intent.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }
    }

    @OnClick({R.id.widget_configure_add_button})
    public void addWidget(View view) {
        final Context context = WmgWidgetConfigureActivity.this;

        int selectedId = mRadioGroup.getCheckedRadioButtonId();
                
        SelectedOption selectedValue = SelectedOption.NONE;

        if(selectedId == mRbSelectBoth.getId()) {
            selectedValue = SelectedOption.BOTH;
        } else if(selectedId == mRbSelectNearOnly.getId()) {
            selectedValue = SelectedOption.NEAR;
        } else if(selectedId == mRbSelectFavoriteOnly.getId()) {
            selectedValue = SelectedOption.FAVORITE;
        }

        if (selectedValue == SelectedOption.NONE){
            Utils.showSnackBar(findViewById(android.R.id.content),
                    getString(R.string.sb_text_widget_no_selection),
                    Snackbar.LENGTH_LONG);
        }
        else {
            saveWidgetPref(context, mAppWidgetId, selectedValue);

            // It is the responsibility of the configuration activity to update the app widget
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            WmgWidget.updateAppWidget(context, appWidgetManager, mAppWidgetId);

            // Make sure we pass back the original appWidgetId
            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            setResult(RESULT_OK, resultValue);
            finish();
        }
    }

    // Write the selected recipe and ingredients to the SharedPreferences object for this widget
    static void saveWidgetPref(Context context, int appWidgetId, SelectedOption option) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(Utils.PREFS_NAME, 0).edit();
        prefs.putInt(PREF_PREFIX_KEY + appWidgetId, option.getValue());
        prefs.apply();
    }

    static Integer getWidgetPref(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(Utils.PREFS_NAME, 0);
        return prefs.getInt(PREF_PREFIX_KEY + appWidgetId, 0);
    }

    static void deletePref(Context context, int appWidgetId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(Utils.PREFS_NAME, 0).edit();
        prefs.remove(PREF_PREFIX_KEY + appWidgetId);
        prefs.apply();
    }

}

