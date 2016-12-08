package com.proxerme.app.module.resolver

import com.proxerme.app.task.StreamResolutionTask.StreamResolutionResult

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ProxerProductStreamResolver : StreamResolver() {
    override val name = "artikel"

    override fun appliesTo(url: String): Boolean {
        return super.appliesTo(url) || url.contains("article", false)
    }

    override fun resolve(url: String): StreamResolutionResult {
        return StreamResolutionResult(if (url.startsWith("//")) "http:" + url else url, "text/html")
    }
}