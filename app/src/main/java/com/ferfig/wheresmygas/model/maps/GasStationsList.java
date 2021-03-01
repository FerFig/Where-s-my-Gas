
package com.ferfig.wheresmygas.model.maps;

import java.util.ArrayList;
import java.util.List;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class GasStationsList implements Parcelable
{

    @SerializedName("html_attributions")
    @Expose
    private List<Object> htmlAttributions = new ArrayList<>();
    @SerializedName("results")
    @Expose
    private List<Result> results = new ArrayList<>();
    @SerializedName("status")
    @Expose
    private String status;
    public final static Parcelable.Creator<GasStationsList> CREATOR = new Creator<GasStationsList>() {


        @SuppressWarnings({
            "unchecked"
        })
        public GasStationsList createFromParcel(Parcel in) {
            return new GasStationsList(in);
        }

        public GasStationsList[] newArray(int size) {
            return (new GasStationsList[size]);
        }

    }
    ;

    @SuppressWarnings("WeakerAccess")
    protected GasStationsList(Parcel in) {
        in.readList(this.htmlAttributions, (java.lang.Object.class.getClassLoader()));
        in.readList(this.results, (Result.class.getClassLoader()));
        this.status = ((String) in.readValue((String.class.getClassLoader())));
    }

    public GasStationsList() {
    }

    public List<Object> getHtmlAttributions() {
        return htmlAttributions;
    }

    public void setHtmlAttributions(List<Object> htmlAttributions) {
        this.htmlAttributions = htmlAttributions;
    }

    public List<Result> getResults() {
        return results;
    }

    public void setResults(List<Result> results) {
        this.results = results;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(htmlAttributions);
        dest.writeList(results);
        dest.writeValue(status);
    }

    public int describeContents() {
        return  0;
    }

}
