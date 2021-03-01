package com.ferfig.wheresmygas.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;

import com.ferfig.wheresmygas.R;
import com.ferfig.wheresmygas.Utils;

public class RateMe {
    private final static int DAYS_UNTIL_PROMPT = 3;
    private final static int LAUNCHES_UNTIL_PROMPT = 7;

    public static void promptToRate(Context mContext) {
        if (Utils.readBooleanSetting(mContext, "dontPromptToRateAgain", false)) {
            return;
        }

        // Increment launch counter
        long launch_count = Utils.readLongSetting(mContext, "promptRateCount")+1;
        if (launch_count < LAUNCHES_UNTIL_PROMPT){
            Utils.saveSetting(mContext, "promptRateCount", launch_count);
        }

        // Get date of first launch
        long date_firstLaunch = Utils.readLongSetting(mContext, "firstRateRequestDate");
        if (date_firstLaunch == 0) {
            date_firstLaunch = System.currentTimeMillis();
            Utils.saveSetting(mContext, "firstRateRequestDate", date_firstLaunch);
        }

        // Wait at least n days before opening
        if (launch_count >= LAUNCHES_UNTIL_PROMPT) {
            if (System.currentTimeMillis() >= date_firstLaunch +
                    (DAYS_UNTIL_PROMPT * 24 * 60 * 60 * 1000)) {
                showRateDialog(mContext);
            }
        }
    }

    private static void showRateDialog(final Context mContext) {
        final AlertDialog.Builder bld = new AlertDialog.Builder(mContext);
        bld.setIcon(R.drawable.ic_launcher_foreground);
        bld.setTitle("Rate " + mContext.getString(R.string.app_name));
        bld.setMessage("Do you like " + mContext.getString(R.string.app_name) + "?\nIt will mean a lot if you take a moment to rate it!\nThank you for your support.");
        bld.setPositiveButton("Rate\n" + mContext.getString(R.string.app_name), new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + mContext.getPackageName())));
            }
        });
        bld.setNeutralButton("Remind me later", new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Utils.saveSetting(mContext, "firstRateRequestDate", System.currentTimeMillis()); //reset date to prompt again in DAYS_UNTIL_PROMPT days
            }
        });
        bld.setNegativeButton("No, thanks", new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Utils.saveSetting(mContext, "dontPromptToRateAgain", true);
            }
        });
        bld.create().show();
    }
}