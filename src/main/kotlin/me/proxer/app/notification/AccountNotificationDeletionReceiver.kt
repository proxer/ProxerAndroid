package me.proxer.app.notification

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import me.proxer.app.util.data.StorageHelper
import java.util.*

/**
 * @author Ruben Gees
 */
class AccountNotificationDeletionReceiver : BroadcastReceiver() {

    companion object {
        fun getPendingIntent(context: Context): PendingIntent = PendingIntent
                .getBroadcast(context, 0, Intent(context, AccountNotificationDeletionReceiver::class.java), 0)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        StorageHelper.lastNotificationsDate = Date()
    }
}
