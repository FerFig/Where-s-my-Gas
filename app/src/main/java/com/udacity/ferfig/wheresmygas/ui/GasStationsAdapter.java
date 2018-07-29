package com.udacity.ferfig.wheresmygas.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.udacity.ferfig.wheresmygas.R;
import com.udacity.ferfig.wheresmygas.Utils;
import com.udacity.ferfig.wheresmygas.model.GasStation;

import java.util.List;

public class GasStationsAdapter extends RecyclerView.Adapter<GasStationViewHolder> {

    private final Context mContext;

    private final List<GasStation> mData;

    public interface OnItemClickListener {
        void onItemClick(GasStation gasStationData);
    }

    public interface OnDirectionsClickListener {
        void onDirectionsClick(GasStation gasStationData);
    }

    public interface OnFavoritesClickListener {
        void onFavoritesClick(GasStation gasStationData);
    }

    private final OnItemClickListener itemClickListener;
    private final OnDirectionsClickListener directionsClickListener;
    private final OnFavoritesClickListener favoritesClickListener;

    public GasStationsAdapter(Context context, List<GasStation> gasStationDataList,
                              OnItemClickListener gasStationClickListener,
                              OnFavoritesClickListener gasStationFavoritesClickListener,
                              OnDirectionsClickListener gasStationDirectionsClickListener) {
        this.mContext = context;
        this.mData = gasStationDataList;
        this.itemClickListener = gasStationClickListener;
        this.directionsClickListener = gasStationDirectionsClickListener;
        this.favoritesClickListener = gasStationFavoritesClickListener;
    }

    @NonNull
    @Override
    public GasStationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater mInftr = LayoutInflater.from(mContext);
        View view = mInftr.inflate(R.layout.gas_station_card_view, parent, false);
        return new GasStationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GasStationViewHolder holder, int position) {
        Log.d(Utils.TAG, "Gas Station: " + mData.get(position).getName() + " ( " + position + " = " + String.valueOf(getItemCount()-1) + " )");
        holder.bind(mData.get(position),
                itemClickListener, directionsClickListener, favoritesClickListener);
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }
}
