package me.proxer.app.chat.sync

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.support.v4.app.RemoteInput
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.MainApplication.Companion.chatDao
import me.proxer.app.MainApplication.Companion.chatDatabase

/**
 * @author Ruben Gees
 */
class DirectReplyReceiver : BroadcastReceiver() {

    companion object {
        const val REMOTE_REPLY_EXTRA = "remote_reply"

        private const val CONFERENCE_ID_EXTRA = "conference_id"

        fun getPendingIntent(context: Context, conferenceId: Long): PendingIntent = PendingIntent.getBroadcast(context,
                conferenceId.toInt(),
                Intent(context, DirectReplyReceiver::class.java)
                        .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                        .apply { putExtra(CONFERENCE_ID_EXTRA, conferenceId) },
                PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val conferenceId = intent.getLongExtra(CONFERENCE_ID_EXTRA, -1)

        Completable
                .fromAction {
                    chatDatabase.insertMessageToSend(getMessageText(intent), conferenceId)

                    if (chatDao.getUnreadConferences().isEmpty()) {
                        ChatNotifications.cancel(context)
                    } else {
                        ChatNotifications.cancelIndividual(context, conferenceId)
                    }

                    ChatJob.scheduleSynchronization()
                }
                .subscribeOn(Schedulers.io())
                .subscribe({}, {})
    }

    private fun getMessageText(intent: Intent) = RemoteInput.getResultsFromIntent(intent)
            .getCharSequence(REMOTE_REPLY_EXTRA)
            .toString()
}
