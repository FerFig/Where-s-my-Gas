package com.ferfig.wheresmygas.model;


import android.location.Location;
import android.location.LocationManager;
import android.os.Parcel;
import android.os.Parcelable;

import com.ferfig.wheresmygas.model.maps.Result;

/* class to store favorite gas stations */
@SuppressWarnings({"unused", "RedundantSuppression"})
public class GasStation implements Parcelable {

    public boolean isSelected;
    private String mId;
    private String mName;
    private String mImageUrl;
    private Double mLatitude;
    private Double mLongitude;
    private String mAddress;
    private Result mDetails;

    public GasStation(String id, String name, String imageUrl, Double latitude, Double longitude, String address, Result details) {
        this.mId = id;
        this.mName = name;
        this.mImageUrl = imageUrl;
        this.mLatitude = latitude;
        this.mLongitude = longitude;
        this.mAddress = address;
        this.mDetails = details;
    }

    /** Parcelable Stuff **/
    protected GasStation(Parcel in) {
        mId = in.readString();
        mName = in.readString();
        mImageUrl = in.readString();
        mLatitude = in.readDouble();
        mLongitude = in.readDouble();
        mAddress = in.readString();
        mDetails = in.readParcelable(Result.class.getClassLoader());
    }

    public static final Creator<GasStation> CREATOR = new Creator<GasStation>() {
        @Override
        public GasStation createFromParcel(Parcel in) {
            return new GasStation(in);
        }

        @Override
        public GasStation[] newArray(int size) {
            return new GasStation[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mId);
        dest.writeString(mName);
        dest.writeString(mImageUrl);
        dest.writeDouble(mLatitude);
        dest.writeDouble(mLongitude);
        dest.writeString(mAddress);
        mDetails.writeToParcel(dest, flags);
    }

    /** getters and setters **/
    public String getPlaceId() {
        return mId;
    }
    public void setId(String id) {
        this.mId = id;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public String getImageUrl() {
        return mImageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.mImageUrl = imageUrl;
    }
    public Double getLatitude() {
        return mLatitude;
    }

    public void setLatitude(Double latitude) {
        this.mLatitude = latitude;
    }

    public Double getLongitude() {
        return mLongitude;
    }

    public void setLongitude(Double longitude) {
        this.mLongitude = longitude;
    }

    public String getAddress() {
        return mAddress;
    }

    public void setAddress(String address) {
        this.mAddress = address;
    }

    public Result getDetails() {
        return mDetails;
    }

    public void setDetails(Result details) {
        this.mDetails = details;
    }

    /**
     * Distance must always be calculated in relation to current or a given location
     * @param location current or specified location
     * @return distance to param location
     */
    public float getDistanceTo(Location location) {
        //
        if (location == null) return -1f;
        Location gasLocation = new Location(LocationManager.GPS_PROVIDER);
        gasLocation.setLatitude(this.getLatitude());
        gasLocation.setLongitude(this.getLongitude());
        return gasLocation.distanceTo(location); // meters
    }

    public GasStationState getState() {
        if (mDetails == null || mDetails.getOpeningHours() == null)
            return GasStationState.UNKNOWN;
        return mDetails.getOpeningHours().getOpenNow() ? GasStationState.OPEN : GasStationState.CLOSED;
    }
}
