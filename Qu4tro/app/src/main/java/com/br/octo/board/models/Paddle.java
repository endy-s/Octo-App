package com.br.octo.board.models;

import org.parceler.Parcel;
import org.parceler.ParcelPropertyConverter;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by endysilveira on 24/06/17.
 */

@Parcel
public class Paddle extends RealmObject {

    @PrimaryKey
    private int id;

    private long date, duration;
    private Integer kcal, rows;
    private float distance, speed;
    @ParcelPropertyConverter(TrackListParcelConverter.class)
    private RealmList<TrackingPoints> track;

    public Paddle() {
    }

//    @ParcelConstructor
//    public Paddle(int id, float distance, long duration, Integer kcal, long date, float speed,
//                  Integer rows, RealmList<TrackingPoints> track) {
//        this.id = id;
//        this.distance = distance;
//        this.duration = duration;
//        this.kcal = kcal;
//        this.date = date;
//        this.speed = speed;
//        this.rows = rows;
//        this.track = track;
//    }


    public int getId() {
        return id;
    }

    public float getDistance() {
        return distance;
    }

    public long getDuration() {
        return duration;
    }

    public Integer getKcal() {
        return kcal;
    }

    public long getDate() {
        return date;
    }

    public float getSpeed() {
        return speed;
    }

    public Integer getRows() {
        return rows;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public void setKcal(Integer kcal) {
        this.kcal = kcal;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public void setRows(Integer rows) {
        this.rows = rows;
    }

    public RealmList<TrackingPoints> getTrack() {
        return track;
    }

    public void setTrack(RealmList<TrackingPoints> track) {
        this.track = track;
    }
}
