package com.proxerme.app.module.resolver

import com.proxerme.app.application.MainApplication
import okhttp3.Request
import java.io.IOException

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class NovamovStreamResolver : StreamResolver() {

    override val name = "novamov"

    private val keyRegex = Regex("file=\"(.*?)\".*filekey=\"(.*?)\"", RegexOption.DOT_MATCHES_ALL)
    private val urlRegex = Regex("url=(.*?)&title")

    override fun resolve(url: String): String {
        val response = MainApplication.proxerConnection.httpClient.newCall(Request.Builder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .get()
                .url(url)
                .build()).execute()
        val regexResult = keyRegex.find(validateAndGetResult(response))
        val file = regexResult?.groupValues?.get(1)
        val fileKey = regexResult?.groupValues?.get(2)

        if (file?.isBlank() ?: true || fileKey?.isBlank() ?: true) {
            throw IOException()
        }

        val apiResponse = MainApplication.proxerConnection.httpClient.newCall(Request.Builder()
                .get()
                .url("http://www.auroravid.to/api/player.api.php?file=%s&key=%s".format(file, fileKey))
                .build()).execute()

        return urlRegex.find(validateAndGetResult(apiResponse))?.groupValues?.get(1)
                ?: throw IOException()
    }
}