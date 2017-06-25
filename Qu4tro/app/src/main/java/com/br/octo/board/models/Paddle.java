package com.br.octo.board.models;

import org.parceler.Parcel;
import org.parceler.ParcelPropertyConverter;

import io.realm.RealmList;
import io.realm.RealmObject;

/**
 * Created by endysilveira on 24/06/17.
 */

@Parcel
public class Paddle extends RealmObject {
    private String distance, duration, kcal, date, speed, rows;
    @ParcelPropertyConverter(TrackListParcelConverter.class)
    private RealmList<TrackingPoints> track;

    public Paddle() {
    }

//    @ParcelConstructor
//    public Paddle(String distance, String duration, String kcal, String date, String speed,
//                  String rows, RealmList<TrackingPoints> track) {
//        this.distance = distance;
//        this.duration = duration;
//        this.kcal = kcal;
//        this.date = date;
//        this.speed = speed;
//        this.rows = rows;
//        this.track = track;
//    }

    public String getDistance() {
        return distance;
    }

    public String getDuration() {
        return duration;
    }

    public String getKcal() {
        return kcal;
    }

    public String getDate() {
        return date;
    }

    public String getSpeed() {
        return speed;
    }

    public String getRows() {
        return rows;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public void setKcal(String kcal) {
        this.kcal = kcal;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setSpeed(String speed) {
        this.speed = speed;
    }

    public void setRows(String rows) {
        this.rows = rows;
    }

    public RealmList<TrackingPoints> getTrack() {
        return track;
    }

    public void setTrack(RealmList<TrackingPoints> track) {
        this.track = track;
    }
}
