package com.ferfig.wheresmygas.provider;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.content.AsyncTaskLoader;

abstract class GasStationsAsyncTaskLoader<T> extends AsyncTaskLoader<T> {
    private T mData;
    private boolean hasResult = false;

    GasStationsAsyncTaskLoader(@NonNull final Context context) {
        super(context);
        onContentChanged();
    }

    @Override
    protected void onStartLoading() {
        if (takeContentChanged()) {
            forceLoad();
        }
        else{
            if (hasResult) {
                deliverResult(mData);
            }
        }

        super.onStartLoading();
    }

    @Override
    public void deliverResult(@Nullable T data) {
        mData = data;
        hasResult = true;
        super.deliverResult(data);
    }

    @Override
    protected void onReset() {
        super.onReset();
        onStopLoading();
        if (hasResult) {
            mData = null;
            hasResult = false;
        }
    }
}
