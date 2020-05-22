package com.huawei.geofencetest;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.huawei.hms.location.Geofence;

import java.io.Serializable;
import java.util.ArrayList;

public class GeofenceSerializable implements Serializable {

    public ArrayList<Geofence> getGeofenceList() {
        return geofenceList;
    }

    public void setGeofenceList(ArrayList<Geofence> geofenceList) {
        this.geofenceList = geofenceList;
    }

    @Expose
    @SerializedName("geofenceList")
    private ArrayList<Geofence> geofenceList = null;

}
