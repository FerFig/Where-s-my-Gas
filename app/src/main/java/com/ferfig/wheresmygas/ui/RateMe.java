package com.ferfig.wheresmygas.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.ferfig.wheresmygas.R;
import com.ferfig.wheresmygas.Utils;

public class RateMe {
    private static final int DAYS_UNTIL_PROMPT = 3;
    private static final int LAUNCHES_UNTIL_PROMPT = 7;
    private static final String FIRST_RATE_REQUEST_DATE = "firstRateRequestDate";

    private RateMe() {}

    public static void promptToRate(Context mContext) {
        if (Utils.readBooleanSetting(mContext, "dontPromptToRateAgain", false)) {
            return;
        }

        // Increment launch counter
        long launchCount = Utils.readLongSetting(mContext, "promptRateCount")+1;
        if (launchCount < LAUNCHES_UNTIL_PROMPT){
            Utils.saveSetting(mContext, "promptRateCount", launchCount);
        }

        // Get date of first launch
        long dateFirstLaunch = Utils.readLongSetting(mContext, FIRST_RATE_REQUEST_DATE);
        if (dateFirstLaunch == 0) {
            dateFirstLaunch = System.currentTimeMillis();
            Utils.saveSetting(mContext, FIRST_RATE_REQUEST_DATE, dateFirstLaunch);
        }

        // Wait at least n days before opening
        if ((launchCount >= LAUNCHES_UNTIL_PROMPT) &&
                (System.currentTimeMillis() >= dateFirstLaunch + (DAYS_UNTIL_PROMPT * 24 * 60 * 60 * 1000))) {
            showRateDialog(mContext);
        }
    }

    private static void showRateDialog(final Context mContext) {
        final AlertDialog.Builder bld = new AlertDialog.Builder(mContext);
        bld.setIcon(R.drawable.ic_launcher_foreground);
        bld.setTitle("Rate " + mContext.getString(R.string.app_name));
        bld.setMessage("Do you like " + mContext.getString(R.string.app_name) + "?\nIt will mean a lot if you take a moment to rate it!\nThank you for your support.");
        bld.setPositiveButton("Rate\n" + mContext.getString(R.string.app_name), (dialog, which) ->
                mContext.startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=" + mContext.getPackageName()))));

        bld.setNeutralButton("Remind me later", (dialog, which) ->
            Utils.saveSetting(mContext, FIRST_RATE_REQUEST_DATE, System.currentTimeMillis())); //reset date to prompt again in DAYS_UNTIL_PROMPT days

        bld.setNegativeButton("No, thanks", (dialog, which) ->
                Utils.saveSetting(mContext, "dontPromptToRateAgain", true));

        bld.create().show();
    }
}