package me.proxer.app.anime.resolver

import android.content.Intent
import android.net.Uri

/**
 * @author Ruben Gees
 */
class StreamResolutionResult(val intent: Intent) {

    companion object {
        const val ACTION_SHOW_MESSAGE = "me.proxer.app.intent.action.SHOW_MESSAGE"
        const val MESSAGE_EXTRA = "message"
    }

    constructor(uri: Uri, mimeType: String) : this(Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType)
    })

    constructor(message: CharSequence) : this(Intent(ACTION_SHOW_MESSAGE).apply {
        putExtra(MESSAGE_EXTRA, message)
    })
}
