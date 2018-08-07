package me.proxer.app.anime.resolver

import io.reactivex.Single
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.androidUri
import me.proxer.app.util.extension.buildSingle

/**
 * @author Ruben Gees
 */
class YouTubeStreamResolver : StreamResolver {

    override val name = "YouTube"
    override val official = true

    override fun resolve(id: String): Single<StreamResolutionResult> = api.anime().link(id)
        .buildSingle()
        .map { StreamResolutionResult(Utils.getAndFixUrl(it).androidUri(), "text/html") }
}
