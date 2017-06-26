package com.br.octo.board.models;

import org.parceler.Parcel;
import org.parceler.ParcelConstructor;

import io.realm.RealmObject;

/**
 * Created by endysilveira on 24/06/17.
 */

@Parcel
public class TrackingPoints extends RealmObject {
    private double latitude;
    private double longitude;

    public TrackingPoints() {
    }

    @ParcelConstructor
    public TrackingPoints(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return this.latitude;
    }

    public double getLongitude() {
        return this.longitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}
