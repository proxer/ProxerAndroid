package me.proxer.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.support.v4.app.RemoteInput
import me.proxer.app.BuildConfig
import me.proxer.app.application.MainApplication.Companion.chatDb
import me.proxer.app.helper.StorageHelper
import me.proxer.app.job.ChatJob

/**
 * @author Ruben Gees
 */
class DirectReplyReceiver : BroadcastReceiver() {

    companion object {
        const val REMOTE_REPLY_EXTRA = "remote_reply"

        private const val CONFERENCE_ID_EXTRA = "conference_id"
        private const val REPLY_ACTION = "${BuildConfig.APPLICATION_ID}.ACTION_MESSAGE_REPLY"

        fun getMessageReplyIntent(conferenceId: String): Intent {
            return Intent().addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                    .setAction(REPLY_ACTION)
                    .putExtra(CONFERENCE_ID_EXTRA, conferenceId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == REPLY_ACTION) {
            StorageHelper.user?.let { user ->
                intent.getStringExtra(CONFERENCE_ID_EXTRA).let {
                    chatDb.insertMessageToSend(user, it, getMessageText(intent))
                }

                ChatJob.schedule()
            }
        }
    }

    private fun getMessageText(intent: Intent) = RemoteInput.getResultsFromIntent(intent)
            .getCharSequence(REMOTE_REPLY_EXTRA)
            .toString()
}