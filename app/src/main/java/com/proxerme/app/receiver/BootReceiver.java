package com.proxerme.app.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.proxerme.app.application.MainApplication;

/**
 * Receiver for a boot. It starts and schedules all background services and tasks.
 *
 * @author Ruben Gees
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            try {
                MainApplication application = (MainApplication) context.getApplicationContext();

                application.getNotificationManager().retrieveNewsLater(context);
                application.getNotificationManager().retrieveMessagesLater(context);
            } catch (ClassCastException e) {
                Log.e(getClass().getName(), "getApplicationContext did not return the " +
                        "Application. Notifications after boot will not work on this device.");
            }
        }
    }
}
