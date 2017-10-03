package com.br.octo.board;

import android.app.Application;
import android.content.Context;

import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;
import io.realm.Realm;

/**
 * Created by Endy on 18/04/2017.
 */

public class MainApplication extends Application {

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.enableCrashlytics) Fabric.with(this, new Crashlytics());
        Realm.init(this);
        context = this;
    }

    public static Context getOCTOContext() {
        return context;
    }
}