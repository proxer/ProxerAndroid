package me.proxer.app.anime.resolver

import io.reactivex.Single
import me.proxer.app.MainApplication.Companion.GENERIC_USER_AGENT
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.MainApplication.Companion.client
import me.proxer.app.MainApplication.Companion.moshi
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.androidUri
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.toBodySingle
import okhttp3.HttpUrl
import okhttp3.Request

/**
 * @author Ruben Gees
 */
class MyviStreamResolver : StreamResolver() {

    override val name = "Myvi"

    override fun resolve(id: String): Single<StreamResolutionResult> = api.anime().link(id)
            .buildSingle()
            .flatMap { url ->
                client.newCall(Request.Builder()
                        .get()
                        .url(apiUrl(Utils.parseAndFixUrl(url)))
                        .header("User-Agent", GENERIC_USER_AGENT)
                        .build())
                        .toBodySingle()
            }
            .map {
                val resultUrl = moshi.adapter(SprutoResult::class.java).fromJson(it)?.url
                        ?: throw  StreamResolutionException()

                StreamResolutionResult(resultUrl.androidUri(), "video/mp4")
            }

    private fun apiUrl(url: HttpUrl) = HttpUrl
            .parse("http://myvi.ru/player/api/Video/Get/${url.pathSegments().last()}?sig")

    private class SprutoResult(private val sprutoData: SprutoData) {
        val url: HttpUrl? get() = HttpUrl.parse(sprutoData.playlist?.first()?.video?.first()?.url)
    }

    private class SprutoData(val playlist: Array<SprutoPlaylistItem>?)
    private class SprutoPlaylistItem(val video: Array<SprutoVideo>?)
    private class SprutoVideo(val url: String?)
}
