package com.proxerme.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.support.v4.app.RemoteInput
import com.proxerme.app.BuildConfig
import com.proxerme.app.data.chatDatabase
import com.proxerme.app.helper.StorageHelper
import com.proxerme.app.service.ChatService

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class DirectReplyReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_REMOTE_REPLY = "extra_remote_reply"
        private const val EXTRA_CONFERENCE_ID = "extra_conference_id"
        private const val REPLY_ACTION = "${BuildConfig.APPLICATION_ID}.ACTION_MESSAGE_REPLY"

        fun getMessageReplyIntent(conferenceId: String): Intent {
            return Intent().addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                    .setAction(REPLY_ACTION)
                    .putExtra(EXTRA_CONFERENCE_ID, conferenceId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (REPLY_ACTION == intent.action) {
            StorageHelper.user?.let {
                val id = intent.getStringExtra(EXTRA_CONFERENCE_ID)

                context.chatDatabase.insertMessageToSend(it, id, getMessageText(intent))

                ChatService.synchronize(context)
            }
        }
    }

    private fun getMessageText(intent: Intent): String {
        return RemoteInput.getResultsFromIntent(intent)
                .getCharSequence(EXTRA_REMOTE_REPLY)
                .toString()
    }
}