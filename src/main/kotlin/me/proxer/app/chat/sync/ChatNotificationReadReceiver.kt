package me.proxer.app.chat.sync

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.MainApplication.Companion.chatDao
import me.proxer.app.util.extension.subscribeAndLogErrors

/**
 * @author Ruben Gees
 */
class ChatNotificationReadReceiver : BroadcastReceiver() {

    companion object {
        private const val CONFERENCE_ID_EXTRA = "conference_id"

        fun getPendingIntent(context: Context, conferenceId: Long): PendingIntent = PendingIntent.getBroadcast(context,
            conferenceId.toInt(),
            Intent(context, ChatNotificationReadReceiver::class.java)
                .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                .apply { putExtra(CONFERENCE_ID_EXTRA, conferenceId) },
            PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val conferenceId = intent.getLongExtra(CONFERENCE_ID_EXTRA, -1L)

        Completable
            .fromAction {
                chatDao.markConferenceAsRead(conferenceId)

                if (chatDao.getUnreadConferences().isEmpty()) {
                    ChatNotifications.cancel(context)
                } else {
                    ChatNotifications.cancelIndividual(context, conferenceId)
                }
            }
            .subscribeOn(Schedulers.io())
            .subscribeAndLogErrors()
    }
}
