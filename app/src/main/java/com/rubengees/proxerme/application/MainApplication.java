package com.rubengees.proxerme.application;

import android.app.Application;

import net.danlew.android.joda.JodaTimeAndroid;

/**
 * Todo: Describe Class
 *
 * @author Ruben Gees
 */
public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        JodaTimeAndroid.init(this);
    }

}
