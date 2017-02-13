package com.proxerme.app.stream.resolver

import android.net.Uri
import com.proxerme.app.stream.StreamResolver
import com.proxerme.app.task.StreamResolutionTask.StreamResolutionResult

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class AkibaPassResolver : StreamResolver() {

    override val name = "AkibaPass"

    override fun resolve(url: String): StreamResolutionResult {
        return StreamResolutionResult(Uri.parse(url), "text/html")
    }
}