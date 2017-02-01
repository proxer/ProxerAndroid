package com.proxerme.app.stream.resolver

import com.proxerme.app.task.StreamResolutionTask.StreamResolutionResult
import com.proxerme.app.util.androidUri
import okhttp3.HttpUrl

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class YoutubeResolver : StreamResolver() {

    override val name = "youtube.com"

    override fun resolve(url: HttpUrl): StreamResolutionResult {
        return StreamResolutionResult(url.androidUri(), "text/html")
    }
}