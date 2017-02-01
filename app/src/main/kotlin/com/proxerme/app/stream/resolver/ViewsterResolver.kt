package com.proxerme.app.stream.resolver

import com.proxerme.app.task.StreamResolutionTask.StreamResolutionResult
import com.proxerme.app.util.androidUri
import okhttp3.HttpUrl

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ViewsterResolver : StreamResolver() {

    override val name = "viewster.com"

    override fun resolve(url: HttpUrl): StreamResolutionResult {
        return StreamResolutionResult(url.androidUri(), "text/html")
    }
}