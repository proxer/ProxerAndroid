package com.proxerme.app.application;

import android.app.Application;

import com.orhanobut.hawk.Hawk;
import com.orhanobut.hawk.HawkBuilder;

import net.danlew.android.joda.JodaTimeAndroid;

/**
 * The {@link Application}, which is used by this App. It does some configuration at start.
 *
 * @author Ruben Gees
 */
public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        JodaTimeAndroid.init(this);
        Hawk.init(this).setEncryptionMethod(HawkBuilder.EncryptionMethod.MEDIUM)
                .setStorage(HawkBuilder.newSharedPrefStorage(this)).build();
    }

}
