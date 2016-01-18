package com.proxerme.app.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.proxerme.app.service.NotificationService;

/**
 * Receiver for the {@link NotificationService}.
 *
 * @author Ruben Gees
 */
public class NotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        //noinspection WrongConstant
        NotificationService.load(context, intent.getAction());
    }
}
