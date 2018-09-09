package me.proxer.app.news

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.library.api.ProxerApi
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import java.util.Date

/**
 * @author Ruben Gees
 */
class NewsNotificationReadReceiver : BroadcastReceiver(), KoinComponent {

    companion object {
        fun getPendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, NewsNotificationReadReceiver::class.java)
                .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private val api by inject<ProxerApi>()

    override fun onReceive(context: Context, intent: Intent?) {
        Completable
            .fromAction {
                NewsNotifications.cancel(context)

                StorageHelper.lastNewsDate = Date()

                api.notifications().news()
                    .markAsRead(true)
                    .limit(0)
                    .build()
                    .execute()
            }
            .subscribeOn(Schedulers.io())
            .subscribeAndLogErrors()
    }
}
