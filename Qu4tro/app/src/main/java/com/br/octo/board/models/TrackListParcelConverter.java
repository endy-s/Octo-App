package com.br.octo.board.models;

import org.parceler.Parcels;

/**
 * Created by endysilveira on 25/06/17.
 */

public class TrackListParcelConverter extends RealmListParcelConverter<TrackingPoints> {

    @Override
    public void itemToParcel(TrackingPoints input, android.os.Parcel parcel) {
        parcel.writeParcelable(Parcels.wrap(input), 0);
    }

    @Override
    public TrackingPoints itemFromParcel(android.os.Parcel parcel) {
        return Parcels.unwrap(parcel.readParcelable(TrackingPoints.class.getClassLoader()));
    }
}

