package com.proxerme.app.stream.resolver

import com.proxerme.app.stream.StreamResolver
import com.proxerme.app.task.StreamResolutionTask.StreamResolutionResult
import okhttp3.HttpUrl

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class CodeResolver : StreamResolver() {

    override val name = "Code"
    private val alternativeName = "Nachricht"

    override fun appliesTo(name: String): Boolean {
        return name.contains(this.name, false) || name.contains(alternativeName, false)
    }

    override fun resolve(url: HttpUrl): StreamResolutionResult {
        return StreamResolutionResult(url.toString().replace(Regex("</?h1>"), ""))
    }
}
