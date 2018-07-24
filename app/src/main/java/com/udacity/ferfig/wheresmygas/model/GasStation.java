package com.udacity.ferfig.wheresmygas.model;


import android.os.Parcel;
import android.os.Parcelable;

/* class to store favorite gas stations */
public final class GasStation implements Parcelable {

    private long mId;
    private String mName;
    private String mLatitude;
    private String mLongitude;
    private String mDetails;

    public GasStation(long mId, String mName, String mLatitude, String mLongitude, String mDetails) {
        this.mId = mId;
        this.mName = mName;
        this.mLatitude = mLatitude;
        this.mLongitude = mLongitude;
        this.mDetails = mDetails;
    }

    /** Parcelable Stuff **/
    protected GasStation(Parcel in) {
        mId = in.readLong();
        mName = in.readString();
        mLatitude = in.readString();
        mLongitude = in.readString();
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
        dest.writeLong(mId);
        dest.writeString(mName);
        dest.writeString(mLatitude);
        dest.writeString(mLongitude);
        dest.writeString(mDetails);
    }

    /** getters and setters **/
    public long getmId() {
        return mId;
    }

    public void setmId(long mId) {
        this.mId = mId;
    }

    public String getmName() {
        return mName;
    }

    public void setmName(String mName) {
        this.mName = mName;
    }

    public String getmLatitude() {
        return mLatitude;
    }

    public void setmLatitude(String mLatitude) {
        this.mLatitude = mLatitude;
    }

    public String getmLongitude() {
        return mLongitude;
    }

    public void setmLongitude(String mLongitude) {
        this.mLongitude = mLongitude;
    }

    public String getmDetails() {
        return mDetails;
    }

    public void setmDetails(String mDetails) {
        this.mDetails = mDetails;
    }
}
