package com.proxerme.app.module.resolver

import com.proxerme.app.application.MainApplication
import okhttp3.Request
import java.io.IOException

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class Mp4UploadResolver : StreamResolver() {

    override val name = "mp4upload"

    private val regex = Regex("\"file\": \"(.+)\"")

    @Throws(IOException::class)
    override fun resolve(url: String): ResolverResult {
        val response = MainApplication.proxerConnection.httpClient.newCall(Request.Builder()
                .get()
                .url(url)
                .build()).execute()

        val result = regex.find(validateAndGetResult(response))?.groupValues?.get(1)
                ?: throw IOException()

        return ResolverResult(result, "video/mp4")
    }
}