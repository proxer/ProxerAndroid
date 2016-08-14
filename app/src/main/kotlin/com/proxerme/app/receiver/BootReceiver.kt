package com.proxerme.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.proxerme.app.helper.ServiceHelper
import com.proxerme.app.service.ChatService

/**
 * Receiver for a boot. It starts and schedules all background services and tasks.

 * @author Ruben Gees
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            ServiceHelper.retrieveNewsLater(context)
            ChatService.reschedule(context)
        }
    }
}
