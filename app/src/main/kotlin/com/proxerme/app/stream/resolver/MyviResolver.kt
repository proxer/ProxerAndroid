package com.proxerme.app.stream.resolver

import com.proxerme.app.stream.StreamResolver
import com.proxerme.app.task.StreamResolutionTask.StreamResolutionResult
import com.proxerme.app.util.ProxerConnectionWrapper
import com.proxerme.app.util.extension.androidUri
import okhttp3.HttpUrl
import okhttp3.Request


/**
 * Resolver for myvi.ru. It is currently not added, as the StreamActivity can not handle the result
 * properly.
 *
 * @author Ruben Gees
 */
class MyviResolver : StreamResolver() {

    override val name = "Myvi"

    override fun resolve(url: HttpUrl): StreamResolutionResult {
        val response = ProxerConnectionWrapper.httpClient.newCall(Request.Builder()
                .get()
                .url("http://myvi.ru/player/api/Video/Get/${url.pathSegments().last()}?sig")
                .build()).execute()

        val resultUrl = ProxerConnectionWrapper.moshi.adapter(SprutoResult::class.java)
                .fromJson(validateAndGetResult(response)).url

        return StreamResolutionResult(resultUrl.androidUri(), "video/mp4")
    }

    private class SprutoResult(private val sprutoData: SprutoData) {
        val url: HttpUrl
            get() = HttpUrl.parse(sprutoData.playlist.first().video.first().url)
    }

    private class SprutoData(val playlist: Array<SprutoPlaylistItem>)
    private class SprutoPlaylistItem(val video: Array<SprutoVideo>)
    private class SprutoVideo(val url: String)
}
