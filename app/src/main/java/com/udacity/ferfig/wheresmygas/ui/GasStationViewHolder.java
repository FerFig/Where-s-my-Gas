package com.udacity.ferfig.wheresmygas.ui;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.udacity.ferfig.wheresmygas.R;
import com.udacity.ferfig.wheresmygas.Utils;
import com.udacity.ferfig.wheresmygas.model.GasStation;

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

    @BindView(R.id.imgGasStationOpen)
    ImageView mImgGasStationImage;

    @BindView(R.id.imgFavorites)
    ImageButton mImgButtonFavorites;

    @BindView(R.id.imgDirections)
    ImageButton mImgButtonDirections;

    public GasStationViewHolder(View itemView) {
        super(itemView);

        ButterKnife.bind(this, itemView);
    }

    public void bind(final GasStation gasStationData,
                     final GasStationsAdapter.OnItemClickListener listener,
                     final GasStationsAdapter.OnDirectionsClickListener directionsClickListener,
                     final GasStationsAdapter.OnFavoritesClickListener favoritesClickListener,
                     final int itemPosition, final int itemsCount) {

        String gasStationName = gasStationData.getName();
        String gasStationAddress = gasStationData.getAddress();
        String gasStationDistance = Utils.formatDistance(gasStationData.getDistance());
//TODO Add gas station image. if time permits...!!!
//        String imageUrl = gasStationData.getImageUrl();
//        if (imageUrl.isEmpty()) {
//            Picasso.get().load(R.mipmap.ic_launcher_round).into(mImgGasStationImage);
//        }
//        else {
//            Picasso.get().load(imageUrl).into(mImgGasStationImage);
//        }
//        //set the content description of the movie image/thumbnail to the movie title ;)
//        mImgGasStationImage.setContentDescription(gasStationName);

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
