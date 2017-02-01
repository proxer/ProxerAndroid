package com.proxerme.app.stream.resolver

import com.proxerme.app.application.MainApplication
import com.proxerme.app.task.StreamResolutionTask
import com.proxerme.app.task.StreamResolutionTask.StreamResolutionResult
import com.proxerme.app.util.androidUri
import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class YouruploadResolver : StreamResolver() {

    override val name = "yourupload.com"

    private val regex = Regex("file: '(.*?)'")

    override fun resolve(url: HttpUrl): StreamResolutionResult {
        val response = MainApplication.proxerConnection.httpClient.newCall(Request.Builder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .get()
                .url(url)
                .build()).execute()
        val regexResult = regex.find(validateAndGetResult(response))
        val file = regexResult?.groupValues?.get(1)

        if (file?.isBlank() ?: true) {
            throw StreamResolutionTask.StreamResolutionException()
        }

        val apiResponse = MainApplication.proxerConnection.httpClient.newCall(Request.Builder()
                .header("Referer", url.toString())
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