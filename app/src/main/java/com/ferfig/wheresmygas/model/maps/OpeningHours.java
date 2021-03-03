
package com.ferfig.wheresmygas.model.maps;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings({"unused", "RedundantSuppression"})
public class OpeningHours implements Parcelable
{

    @SerializedName("open_now")
    @Expose
    private Boolean openNow;
    public final static Parcelable.Creator<OpeningHours> CREATOR = new Creator<OpeningHours>() {

        public OpeningHours createFromParcel(Parcel in) {
            return new OpeningHours(in);
        }

        public OpeningHours[] newArray(int size) {
            return (new OpeningHours[size]);
        }

    }
    ;

    @SuppressWarnings("WeakerAccess")
    protected OpeningHours(Parcel in) {
        this.openNow = ((Boolean) in.readValue((Boolean.class.getClassLoader())));
    }

    public OpeningHours() {
    }

    public Boolean getOpenNow() {
        return openNow;
    }

    public void setOpenNow(Boolean openNow) {
        this.openNow = openNow;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(openNow);
    }

    public int describeContents() {
        return  0;
    }

}
