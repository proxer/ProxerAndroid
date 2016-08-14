package com.proxerme.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.proxerme.app.service.ChatService

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ChatReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (!ChatService.isSynchronizing) {
            ChatService.synchronize(context)
        }
    }
}