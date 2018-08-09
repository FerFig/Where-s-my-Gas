package com.udacity.ferfig.wheresmygas.job;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;

import java.lang.ref.WeakReference;

public class GasStationsUpdateService extends JobService {
    private UpdateTask mGasStationJobTask;

    @Override
    public boolean onStartJob(final JobParameters jobParams) {
        mGasStationJobTask = new UpdateTask(
                new WeakReference<>(GasStationsUpdateService.this), jobParams
        );
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
}
