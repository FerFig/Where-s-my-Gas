package com.udacity.ferfig.wheresmygas.model;


import android.os.Parcel;
import android.os.Parcelable;

/* class to store favorite gas stations */
public final class GasStation implements Parcelable {

    private String mName;
    private Double mLatitude;
    private Double mLongitude;
    private String mDetails;

    public GasStation(String mName, Double mLatitude, Double mLongitude, String mDetails) {
        this.mName = mName;
        this.mLatitude = mLatitude;
        this.mLongitude = mLongitude;
        this.mDetails = mDetails;
    }

    /** Parcelable Stuff **/
    protected GasStation(Parcel in) {
        mName = in.readString();
        mLatitude = in.readDouble();
        mLongitude = in.readDouble();
        mDetails = in.readString();
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
        dest.writeString(mName);
        dest.writeDouble(mLatitude);
        dest.writeDouble(mLongitude);
        dest.writeString(mDetails);
    }

    /** getters and setters **/
    public String getName() {
        return mName;
    }

    public void setName(String mName) {
        this.mName = mName;
    }

    public Double getLatitude() {
        return mLatitude;
    }

    public void setLatitude(Double mLatitude) {
        this.mLatitude = mLatitude;
    }

    public Double getLongitude() {
        return mLongitude;
    }

    public void setLongitude(Double mLongitude) {
        this.mLongitude = mLongitude;
    }

    public String getDetails() {
        return mDetails;
    }

    public void setDetails(String mDetails) {
        this.mDetails = mDetails;
    }
}
