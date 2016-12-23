package com.proxerme.app.stream.resolver

import com.proxerme.app.task.StreamResolutionTask
import com.proxerme.app.util.androidUri
import okhttp3.HttpUrl

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class AnimeOnDemandResolver : StreamResolver() {

    override val name = "anime-on-demand.de"

    override fun resolve(url: HttpUrl): StreamResolutionTask.StreamResolutionResult {
        return StreamResolutionTask.StreamResolutionResult(url.androidUri(), "text/html")
    }
}