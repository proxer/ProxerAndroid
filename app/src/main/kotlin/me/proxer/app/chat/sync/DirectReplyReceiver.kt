package me.proxer.app.chat.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.support.v4.app.RemoteInput
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.BuildConfig
import me.proxer.app.MainApplication.Companion.chatDatabase

/**
 * @author Ruben Gees
 */
class DirectReplyReceiver : BroadcastReceiver() {

    companion object {
        const val REMOTE_REPLY_EXTRA = "remote_reply"

        private const val CONFERENCE_ID_EXTRA = "conference_id"
        private const val REPLY_ACTION = "${BuildConfig.APPLICATION_ID}.ACTION_MESSAGE_REPLY"

        fun getMessageReplyIntent(conferenceId: Long): Intent {
            return Intent().addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                    .setAction(REPLY_ACTION)
                    .putExtra(CONFERENCE_ID_EXTRA, conferenceId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == REPLY_ACTION) {
            intent.getLongExtra(CONFERENCE_ID_EXTRA, 0).let {
                Completable
                        .fromAction {
                            chatDatabase.insertMessageToSend(getMessageText(intent), it)

                            ChatJob.scheduleSynchronization()
                        }
                        .subscribeOn(Schedulers.io())
                        .subscribe()
            }
        }
    }

    private fun getMessageText(intent: Intent) = RemoteInput.getResultsFromIntent(intent)
            .getCharSequence(REMOTE_REPLY_EXTRA)
            .toString()
}