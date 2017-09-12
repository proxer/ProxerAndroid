package me.proxer.app.news

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.MainApplication

/**
 * @author Ruben Gees
 */
class NewsNotificationReadReceiver : BroadcastReceiver() {

    companion object {
        fun getPendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(context, 0,
                Intent(context, NewsNotificationReadReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onReceive(context: Context, intent: Intent?) {
        Completable
                .fromAction {
                    NewsNotifications.cancel(context)

                    MainApplication.api.notifications().news()
                            .markAsRead(true)
                            .build()
                            .execute()
                }
                .subscribeOn(Schedulers.io())
                .subscribe({}, {})
    }
}
