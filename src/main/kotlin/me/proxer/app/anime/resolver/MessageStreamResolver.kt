package me.proxer.app.anime.resolver

import androidx.core.text.parseAsHtml
import io.reactivex.Single
import me.proxer.app.util.extension.buildSingle

/**
 * @author Ruben Gees
 */
class MessageStreamResolver : StreamResolver() {

    override val name = "Nachricht"
    override val resolveEarly = true

    override fun resolve(id: String): Single<StreamResolutionResult> = api.anime.link(id)
        .buildSingle()
        .map { (content, _) -> StreamResolutionResult.Message(content.trim().parseAsHtml().trim()) }
}
