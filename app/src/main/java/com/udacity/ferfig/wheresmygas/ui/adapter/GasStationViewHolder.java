package com.udacity.ferfig.wheresmygas.ui.adapter;

import android.content.Context;
import android.location.Location;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.udacity.ferfig.wheresmygas.R;
import com.udacity.ferfig.wheresmygas.Utils;
import com.udacity.ferfig.wheresmygas.model.GasStation;
import com.udacity.ferfig.wheresmygas.model.maps.Result;
import com.udacity.ferfig.wheresmygas.ui.adapter.GasStationsAdapter;

import butterknife.BindView;
import butterknife.ButterKnife;

public class GasStationViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.gasStationCardView)
    CardView mGasStationCardView;

    @BindView(R.id.tvGasStationName)
    TextView mTvGasStationName;

    @BindView(R.id.tvGasStationAddress)
    TextView mTvGasStationAddress;

    @BindView(R.id.tvGasStationDistance)
    TextView mTvGasStationDistance;

    @BindView(R.id.imgFavorites)
    ImageButton mImgButtonFavorites;

    @BindView(R.id.imgDirections)
    ImageButton mImgButtonDirections;

    public GasStationViewHolder(View itemView) {
        super(itemView);

        ButterKnife.bind(this, itemView);
    }

    public void bind(Context context, final GasStation gasStationData,
                     final Location lastKnownLocation,
                     final GasStationsAdapter.OnItemClickListener listener,
                     final GasStationsAdapter.OnFavoritesClickListener favoritesClickListener,
                     final GasStationsAdapter.OnDirectionsClickListener directionsClickListener,
                     final boolean favoriteGasStation) {

        String gasStationName = gasStationData.getName();
        String gasStationAddress = gasStationData.getAddress();
        String gasStationDistance = "";
        if (lastKnownLocation!= null) {
            gasStationDistance = Utils.formatDistance(context,
                    gasStationData.getDistanceTo(lastKnownLocation));
        }

        Result gasStationDetails = gasStationData.getDetails();
        int imgResource;
        if (gasStationDetails.getOpeningHours() != null) {
            if (gasStationDetails.getOpeningHours().getOpenNow()) {
                // open now
                imgResource =  R.drawable.ic_open_now;
                //mTvGasStationName.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_open_now, 0,0,0);
            } else {
                // not opened
                imgResource = R.drawable.ic_closed;
              //  marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN));
            }
        } else { // unknow if it's open or not! show dimmed/different color...
            imgResource = R.drawable.ic_open_unknown;
        }
        mTvGasStationName.setCompoundDrawablesWithIntrinsicBounds(imgResource, 0,0,0);
        mTvGasStationName.setText(gasStationName);

        mTvGasStationAddress.setText(gasStationAddress);
        mTvGasStationDistance.setText(gasStationDistance);

        itemView.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (listener!=null) {
                    listener.onItemClick(gasStationData);
                }
            }
        });
        if (favoriteGasStation) {
            mImgButtonFavorites.setImageDrawable(ContextCompat.getDrawable(mImgButtonFavorites.getContext(), R.drawable.ic_favorite_on_24dp));
        }else{
            mImgButtonFavorites.setImageDrawable(ContextCompat.getDrawable(mImgButtonFavorites.getContext(), R.drawable.ic_favorite_off_24dp));
        }
        mImgButtonFavorites.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (favoritesClickListener!=null) {
                    favoritesClickListener.onFavoritesClick(gasStationData);
                }
            }
        });
        mImgButtonDirections.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (directionsClickListener!=null) {
                    directionsClickListener.onDirectionsClick(gasStationData);
                }
            }
        });
    }
}
