package me.proxer.app.news

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.reactivex.schedulers.Schedulers
import me.proxer.app.MainApplication
import me.proxer.app.util.extension.buildSingle

/**
 * @author Ruben Gees
 */
class NewsNotificationReadReceiver : BroadcastReceiver() {

    companion object {
        fun getPendingIntent(context: Context): PendingIntent = PendingIntent
                .getBroadcast(context, 0, Intent(context, NewsNotificationReadReceiver::class.java), 0)
    }

    override fun onReceive(context: Context, intent: Intent?) {
        MainApplication.api.notifications().news()
                .markAsRead(true)
                .buildSingle()
                .subscribeOn(Schedulers.io())
                .subscribe({}, {})

        NewsNotifications.cancel(context)
    }
}
