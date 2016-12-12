package com.proxerme.app.stream.resolver

import com.proxerme.app.task.StreamResolutionTask.StreamResolutionResult
import okhttp3.HttpUrl

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ProxerProductStreamResolver : StreamResolver() {

    override val name = "artikel"

    override fun appliesTo(url: HttpUrl): Boolean {
        return url.host().contains("proxer.me", true) && url.pathSegments().firstOrNull() == "article"
    }

    override fun resolve(url: HttpUrl): StreamResolutionResult {
        return StreamResolutionResult(url, "text/html")
    }
}