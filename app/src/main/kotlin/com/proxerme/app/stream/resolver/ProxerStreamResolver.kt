package com.proxerme.app.stream.resolver

import android.net.Uri
import com.proxerme.app.stream.StreamResolver
import com.proxerme.app.task.StreamResolutionTask.StreamResolutionException
import com.proxerme.app.task.StreamResolutionTask.StreamResolutionResult
import com.proxerme.app.util.ProxerConnectionWrapper
import okhttp3.HttpUrl
import okhttp3.Request

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ProxerStreamResolver : StreamResolver() {

    override val name = "Proxer-Stream"

    private val regex = Regex("<source type=\"(.*?)\" src=\"(.*?)\">")

    override fun resolve(url: HttpUrl): StreamResolutionResult {
        val response = ProxerConnectionWrapper.httpClient.newCall(Request.Builder()
                .get()
                .url(url)
                .build()).execute()

        val regexResult = regex.find(validateAndGetResult(response))
                ?: throw StreamResolutionException()
        val type = regexResult.groupValues[1]
        val result = Uri.parse(regexResult.groupValues[2])

        return StreamResolutionResult(result, type)
    }
}
