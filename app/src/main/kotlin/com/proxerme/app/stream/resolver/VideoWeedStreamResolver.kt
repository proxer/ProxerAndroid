package com.proxerme.app.stream.resolver

import android.net.Uri
import com.proxerme.app.stream.StreamResolver
import com.proxerme.app.task.StreamResolutionTask.StreamResolutionException
import com.proxerme.app.task.StreamResolutionTask.StreamResolutionResult
import com.proxerme.app.util.ProxerConnectionWrapper
import okhttp3.Request

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class VideoWeedStreamResolver : StreamResolver() {

    override val name = "VideoWeed"

    private val keyRegex = Regex("fkz=\"(.*?)\".*file=\"(.*?)\"", RegexOption.DOT_MATCHES_ALL)
    private val urlRegex = Regex("url=(.*?)&title")

    override fun resolve(url: String): StreamResolutionResult {
        val response = ProxerConnectionWrapper.httpClient.newCall(Request.Builder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .get()
                .url(url)
                .build()).execute()

        val regexResult = keyRegex.find(validateAndGetResult(response))
        val file = regexResult?.groupValues?.get(2)
        val fileKey = regexResult?.groupValues?.get(1)

        if (file?.isBlank() ?: true || fileKey?.isBlank() ?: true) {
            throw StreamResolutionException()
        }

        val apiResponse = ProxerConnectionWrapper.httpClient.newCall(Request.Builder()
                .get()
                .url("http://www.bitvid.sx/api/player.api.php?file=%s&key=%s".format(file, fileKey))
                .build()).execute()

        val result = Uri.parse(urlRegex.find(validateAndGetResult(apiResponse))
                ?.groupValues?.get(1) ?: throw StreamResolutionException())

        return StreamResolutionResult(result, "video/x-flv")
    }
}
