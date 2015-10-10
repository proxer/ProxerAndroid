package com.proxerme.app.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.proxerme.app.manager.NewsManager;

/**
 * Receiver for a boot. It starts and schedules all background services and tasks.
 *
 * @author Ruben Gees
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            NewsManager.getInstance(context).retrieveNewsLater();
        }
    }
}
