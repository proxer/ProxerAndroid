package com.proxerme.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.proxerme.app.service.NotificationService

/**
 * Receiver for the [NotificationService].

 * @author Ruben Gees
 */
class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        NotificationService.load(context, intent.action)
    }
}
