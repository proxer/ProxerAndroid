package com.proxerme.app.stream.resolver

import android.net.Uri
import com.proxerme.app.application.MainApplication
import com.proxerme.app.stream.StreamResolver
import com.proxerme.app.task.StreamResolutionTask.StreamResolutionException
import com.proxerme.app.task.StreamResolutionTask.StreamResolutionResult
import okhttp3.Request
import java.io.IOException

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class Mp4UploadResolver : StreamResolver() {

    override val name = "MP4Upload"

    private val regex = Regex("\"file\": \"(.+)\"")

    @Throws(IOException::class)
    override fun resolve(url: String): StreamResolutionResult {
        val response = MainApplication.httpClient.newCall(Request.Builder()
                .get()
                .url(url)
                .build()).execute()

        val result = Uri.parse(regex.find(validateAndGetResult(response))?.groupValues?.get(1))
                ?: throw StreamResolutionException()

        return StreamResolutionResult(result, "video/mp4")
    }
}