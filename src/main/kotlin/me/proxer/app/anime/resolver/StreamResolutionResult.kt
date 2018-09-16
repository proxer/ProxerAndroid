package me.proxer.app.anime.resolver

import android.content.Intent
import android.net.Uri
import me.proxer.app.util.extension.newTask

/**
 * @author Ruben Gees
 */
data class StreamResolutionResult(val intent: Intent) {

    companion object {
        const val ACTION_SHOW_MESSAGE = "me.proxer.app.intent.action.SHOW_MESSAGE"
        const val MESSAGE_EXTRA = "message"
        const val REFERER_EXTRA = "referer"
    }

    constructor(uri: Uri, mimeType: String, referer: String? = null) : this(Intent(Intent.ACTION_VIEW).newTask().apply {
        setDataAndType(uri, mimeType)

        if (referer != null) {
            putExtra(REFERER_EXTRA, referer)
        }
    })

    constructor(message: CharSequence) : this(Intent(ACTION_SHOW_MESSAGE).apply {
        putExtra(MESSAGE_EXTRA, message)
    })
}
