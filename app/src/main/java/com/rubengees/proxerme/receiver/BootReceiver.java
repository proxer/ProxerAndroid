package com.rubengees.proxerme.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.rubengees.proxerme.manager.NewsManager;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            NewsManager.getInstance(context).retrieveNewsLater();
        }
    }
}
