
package com.udacity.ferfig.wheresmygas.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class PlusCode implements Parcelable
{

    @SerializedName("compound_code")
    @Expose
    private String compoundCode;
    @SerializedName("global_code")
    @Expose
    private String globalCode;
    public final static Parcelable.Creator<PlusCode> CREATOR = new Creator<PlusCode>() {


        @SuppressWarnings({
            "unchecked"
        })
        public PlusCode createFromParcel(Parcel in) {
            return new PlusCode(in);
        }

        public PlusCode[] newArray(int size) {
            return (new PlusCode[size]);
        }

    }
    ;

    @SuppressWarnings("WeakerAccess")
    protected PlusCode(Parcel in) {
        this.compoundCode = ((String) in.readValue((String.class.getClassLoader())));
        this.globalCode = ((String) in.readValue((String.class.getClassLoader())));
    }

    public PlusCode() {
    }

    public String getCompoundCode() {
        return compoundCode;
    }

    public void setCompoundCode(String compoundCode) {
        this.compoundCode = compoundCode;
    }

    public String getGlobalCode() {
        return globalCode;
    }

    public void setGlobalCode(String globalCode) {
        this.globalCode = globalCode;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(compoundCode);
        dest.writeValue(globalCode);
    }

    public int describeContents() {
        return  0;
    }

}
