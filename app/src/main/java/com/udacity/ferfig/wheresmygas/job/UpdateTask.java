package com.udacity.ferfig.wheresmygas.job;

import android.content.Context;
import android.os.AsyncTask;

import com.firebase.jobdispatcher.JobParameters;

import java.lang.ref.WeakReference;

public class UpdateTask extends AsyncTask<Void, Void, Void> {

    private final WeakReference<Context> mWeakContext;
    private final WeakReference<GasStationsUpdateService> mWeakJobContext;
    private final JobParameters mJobParameters;

    UpdateTask(WeakReference<GasStationsUpdateService> weakContext, JobParameters jobParams) {
        this.mWeakJobContext = weakContext;
        this.mJobParameters = jobParams;
        this.mWeakContext = null;
    }

    UpdateTask(WeakReference<Context> weakContext) {
        this.mJobParameters = null;
        this.mWeakJobContext = null;
        this.mWeakContext = weakContext;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        if (this.mWeakJobContext != null) {
            SyncUtils.executeWheresMyGasJob(mWeakJobContext.get());
        }
        else if (this.mWeakContext != null){
            SyncUtils.executeWheresMyGasJob(mWeakContext.get());
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        if (this.mWeakJobContext != null && this.mJobParameters != null) {
            this.mWeakJobContext.get().jobFinished(this.mJobParameters, false);
        }
    }
}
