package me.proxer.app.news

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.extension.safeInject
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.library.ProxerApi
import org.koin.core.KoinComponent
import org.threeten.bp.Instant

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

    private val api by safeInject<ProxerApi>()
    private val preferenceHelper by safeInject<PreferenceHelper>()

    override fun onReceive(context: Context, intent: Intent?) {
        Completable
            .fromAction {
                NewsNotifications.cancel(context)

                preferenceHelper.lastNewsDate = Instant.now()

                api.notifications.news()
                    .markAsRead(true)
                    .limit(0)
                    .build()
                    .execute()
            }
            .subscribeOn(Schedulers.io())
            .subscribeAndLogErrors()
    }
}
