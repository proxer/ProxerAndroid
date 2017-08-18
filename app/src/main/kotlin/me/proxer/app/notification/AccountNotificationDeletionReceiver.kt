package me.proxer.app.notification

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.reactivex.schedulers.Schedulers
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.util.extension.buildSingle

/**
 * @author Ruben Gees
 */
class AccountNotificationDeletionReceiver : BroadcastReceiver() {

    companion object {
        fun getPendingIntent(context: Context): PendingIntent = PendingIntent
                .getBroadcast(context, 0, Intent(context, AccountNotificationDeletionReceiver::class.java), 0)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        api.notifications().notifications()
                .limit(0)
                .markAsRead(true)
                .buildSingle()
                .subscribeOn(Schedulers.io())
                .subscribe({}, {})
    }
}
