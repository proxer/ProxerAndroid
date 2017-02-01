package com.proxerme.app.stream.resolver

import android.content.Intent
import android.net.Uri
import com.proxerme.app.task.StreamResolutionTask.StreamResolutionException
import com.proxerme.app.task.StreamResolutionTask.StreamResolutionResult
import okhttp3.HttpUrl

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ClipfishResolver : StreamResolver() {

    override val name = "clipfish.de"

    private val regex = Regex("video/(\\d+)?")

    override fun resolve(url: HttpUrl): StreamResolutionResult {
        val id = regex.find(url.toString())?.groupValues?.get(1)
                ?: throw StreamResolutionException()

        return StreamResolutionResult(Intent(Intent.ACTION_VIEW,
                Uri.parse("clipfish://video/$id?ref=proxer")))
        // TODO: Not found action
    }
}