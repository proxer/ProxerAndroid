package me.proxer.app.chat.prv.sync

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.MainApplication.Companion.messengerDao
import me.proxer.app.util.extension.subscribeAndLogErrors

/**
 * @author Ruben Gees
 */
class MessengerNotificationReadReceiver : BroadcastReceiver() {

    companion object {
        private const val CONFERENCE_ID_EXTRA = "conference_id"

        fun getPendingIntent(context: Context, conferenceId: Long): PendingIntent {
            val intent = Intent(context, MessengerNotificationReadReceiver::class.java)
                .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                .apply { putExtra(CONFERENCE_ID_EXTRA, conferenceId) }

            return PendingIntent.getBroadcast(context, conferenceId.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val conferenceId = intent.getLongExtra(CONFERENCE_ID_EXTRA, -1L)

        Completable
            .fromAction {
                messengerDao.markConferenceAsRead(conferenceId)

                val unreadMap = messengerDao.getUnreadConferences()
                    .associate {
                        it to messengerDao.getMostRecentMessagesForConference(it.id, it.unreadMessageAmount)
                            .asReversed()
                    }
                    .plus(messengerDao.getConference(conferenceId) to emptyList())

                MessengerNotifications.showOrUpdate(context, unreadMap)
            }
            .subscribeOn(Schedulers.io())
            .subscribeAndLogErrors()
    }
}
