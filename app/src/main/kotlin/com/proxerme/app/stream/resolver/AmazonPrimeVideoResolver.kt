package com.proxerme.app.stream.resolver

import com.proxerme.app.task.StreamResolutionTask.StreamResolutionResult
import com.proxerme.app.util.androidUri
import okhttp3.HttpUrl

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class AmazonPrimeVideoResolver : StreamResolver() {

    override val name = "amazon.de"

    override fun resolve(url: HttpUrl): StreamResolutionResult {
        return StreamResolutionResult(url.androidUri(), "text/html")
    }
}