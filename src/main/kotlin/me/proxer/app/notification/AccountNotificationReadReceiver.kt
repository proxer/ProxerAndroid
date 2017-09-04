package me.proxer.app.notification

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.reactivex.schedulers.Schedulers
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.util.extension.buildSingle
import me.proxer.library.enums.NotificationFilter

/**
 * @author Ruben Gees
 */
class AccountNotificationReadReceiver : BroadcastReceiver() {

    companion object {
        fun getPendingIntent(context: Context): PendingIntent = PendingIntent
                .getBroadcast(context, 0, Intent(context, AccountNotificationReadReceiver::class.java), 0)
    }

    override fun onReceive(context: Context, intent: Intent?) {
        api.notifications().notifications()
                .limit(Int.MAX_VALUE)
                .markAsRead(true)
                .filter(NotificationFilter.UNREAD)
                .buildSingle()
                .subscribeOn(Schedulers.io())
                .subscribe({}, {})

        AccountNotifications.cancel(context)
    }
}
