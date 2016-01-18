package com.proxerme.app.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.proxerme.app.manager.NotificationRetrievalManager;

/**
 * Receiver for a boot. It starts and schedules all background services and tasks.
 *
 * @author Ruben Gees
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationRetrievalManager.retrieveNewsLater(context);
        NotificationRetrievalManager.retrieveMessagesLater(context);
    }
}
