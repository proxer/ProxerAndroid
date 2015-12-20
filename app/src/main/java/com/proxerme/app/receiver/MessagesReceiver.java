package com.proxerme.app.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.proxerme.app.service.NotificationService;

public class MessagesReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationService.startActionLoadMessages(context);
    }
}
