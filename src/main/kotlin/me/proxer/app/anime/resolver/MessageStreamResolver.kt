package me.proxer.app.anime.resolver

import io.reactivex.Single
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.util.compat.HtmlCompat
import me.proxer.app.util.extension.buildSingle

/**
 * @author Ruben Gees
 */
class MessageStreamResolver : StreamResolver {

    override val name = "Nachricht"

    override fun resolve(id: String): Single<StreamResolutionResult> = api.anime().link(id)
            .buildSingle()
            .map { StreamResolutionResult(HtmlCompat.fromHtml(it).trim()) }
}
