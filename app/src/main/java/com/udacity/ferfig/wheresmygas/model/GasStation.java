package com.udacity.ferfig.wheresmygas.model;


import android.os.Parcel;
import android.os.Parcelable;

import com.udacity.ferfig.wheresmygas.model.maps.Result;

/* class to store favorite gas stations */
public class GasStation implements Parcelable {

    private String mId;
    private String mName;
    private String mImageUrl;
    private Double mLatitude;
    private Double mLongitude;
    private Float mDistance;
    private String mAddress;
    private Result mDetails;

    public GasStation(String id, String name, String imageUrl, Double latitude, Double longitude, Float distance, String address, Result details) {
        this.mId = id;
        this.mName = name;
        this.mImageUrl = imageUrl;
        this.mLatitude = latitude;
        this.mLongitude = longitude;
        this.mDistance = distance;
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
        mDistance = in.readFloat();
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
        dest.writeFloat(mDistance);
        dest.writeString(mAddress);
        mDetails.writeToParcel(dest, flags);
    }

    /** getters and setters **/
    public String getId() {
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

    public Float getDistance() {
        return mDistance;
    }

    public void setDistance(Float distance) {
        this.mDistance = distance;
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
}
