package me.proxer.app.notification

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.library.api.ProxerApi
import me.proxer.library.enums.NotificationFilter
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

/**
 * @author Ruben Gees
 */
class AccountNotificationReadReceiver : BroadcastReceiver(), KoinComponent {

    companion object {
        fun getPendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, AccountNotificationReadReceiver::class.java)
                .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private val api by inject<ProxerApi>()

    override fun onReceive(context: Context, intent: Intent?) {
        Completable
            .fromAction {
                AccountNotifications.cancel(context)

                api.notifications().notifications()
                    .limit(Int.MAX_VALUE)
                    .markAsRead(true)
                    .filter(NotificationFilter.UNREAD)
                    .build()
                    .execute()
            }
            .subscribeOn(Schedulers.io())
            .subscribeAndLogErrors()
    }
}
