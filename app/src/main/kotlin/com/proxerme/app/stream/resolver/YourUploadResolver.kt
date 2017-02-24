package com.proxerme.app.stream.resolver

import com.proxerme.app.stream.StreamResolver
import com.proxerme.app.task.StreamResolutionTask
import com.proxerme.app.task.StreamResolutionTask.StreamResolutionResult
import com.proxerme.app.util.ProxerConnectionWrapper
import com.proxerme.app.util.extension.androidUri
import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class YourUploadResolver : StreamResolver() {

    override val name = "YourUpload"

    private val regex = Regex("file: '(.*?)'")

    override fun resolve(url: String): StreamResolutionResult {
        val response = ProxerConnectionWrapper.httpClient.newCall(Request.Builder()
                .get()
                .url(url)
                .build()).execute()

        val regexResult = regex.find(validateAndGetResult(response))
        val file = regexResult?.groupValues?.get(1)

        if (file?.isBlank() ?: true) {
            throw StreamResolutionTask.StreamResolutionException()
        }

        val apiResponse = ProxerConnectionWrapper.httpClient.newCall(Request.Builder()
                .header("Referer", url)
                .head()
                .url(HttpUrl.parse("http://yourupload.com$file"))
                .build()).execute()

        return StreamResolutionResult(validateAndGetUrl(apiResponse).androidUri(), "video/mp4")
    }

    private fun validateAndGetUrl(response: Response): HttpUrl {
        if (response.isSuccessful) {
            val url = response.networkResponse().request().url()

            response.close()

            return url
        } else {
            throw IOException()
        }
    }
}
