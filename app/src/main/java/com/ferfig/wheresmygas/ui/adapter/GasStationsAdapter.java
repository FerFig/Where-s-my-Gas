package com.ferfig.wheresmygas.ui.adapter;

import android.content.Context;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ferfig.wheresmygas.R;
import com.ferfig.wheresmygas.Utils;
import com.ferfig.wheresmygas.model.GasStation;

import java.util.ArrayList;
import java.util.List;

public class GasStationsAdapter extends RecyclerView.Adapter<GasStationViewHolder> {

    private final Context mContext;

    private static Location mLastKnownLocation;

    private final List<GasStation> mData;

    private static String mSelectedGasStation;

    private final ArrayList<GasStation> mFavoriteGasStations;

    public interface OnItemClickListener {
        void onItemClick(GasStation gasStationData);
    }

    public interface OnFavoritesClickListener {
        void onFavoritesClick(GasStation gasStationData);
    }

    public interface OnDirectionsClickListener {
        void onDirectionsClick(GasStation gasStationData);
    }

    private final OnItemClickListener itemClickListener;
    private final OnFavoritesClickListener favoritesClickListener;
    private final OnDirectionsClickListener directionsClickListener;

    public GasStationsAdapter(Context context, List<GasStation> gasStationDataList,
                              ArrayList<GasStation> favoriteGasStations,
                              Location lastKnownLocation,
                              OnItemClickListener gasStationClickListener,
                              OnFavoritesClickListener gasStationFavoritesClickListener,
                              OnDirectionsClickListener gasStationDirectionsClickListener) {
        this.mContext = context;
        this.mData = gasStationDataList;
        this.mFavoriteGasStations = favoriteGasStations;
        mLastKnownLocation = lastKnownLocation;
        this.itemClickListener = gasStationClickListener;
        this.favoritesClickListener = gasStationFavoritesClickListener;
        this.directionsClickListener = gasStationDirectionsClickListener;
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
        holder.bind(mContext, mData.get(position), mLastKnownLocation,
                itemClickListener, favoritesClickListener, directionsClickListener,
                Utils.isFavoriteGasStation(mData.get(position).getPlaceId(), mFavoriteGasStations));
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public List<GasStation> getData() {
        if (mData == null ) {
            return new ArrayList<>();
        }
        return mData;
    }

    public static String getSelectedGasStationPlaceId() {
        return mSelectedGasStation;
    }

    public static void setSelectedGasStationPlaceId(String gasStationID) {
        GasStationsAdapter.mSelectedGasStation = gasStationID;
    }
}
