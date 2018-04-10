package me.proxer.app.anime.resolver

import android.content.Intent
import android.net.Uri
import io.reactivex.Single
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.MainApplication.Companion.globalContext
import me.proxer.app.exception.AppRequiredException
import me.proxer.app.exception.StreamResolutionException
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.buildSingle

/**
 * @author Ruben Gees
 */
class CrunchyrollStreamResolver : StreamResolver {

    private companion object {
        private const val CRUNCHYROLL_PACKAGE = "com.crunchyroll.crunchyroid"
        private val regex = Regex("media_id=(\\d*)?")
    }

    override val name = "Crunchyroll"

    override fun supports(name: String) = name.startsWith(this.name, true)
    override fun resolve(id: String): Single<StreamResolutionResult> = Single
        .fromCallable {
            if (!Utils.isPackageInstalled(globalContext.packageManager, CRUNCHYROLL_PACKAGE)) {
                throw AppRequiredException(name, CRUNCHYROLL_PACKAGE)
            }
        }
        .flatMap { api.anime().link(id).buildSingle() }
        .map { url ->
            val regexResult = regex.find(url) ?: throw StreamResolutionException()
            val mediaId = regexResult.groupValues[1]

            if (mediaId.isBlank()) {
                throw StreamResolutionException()
            }

            val uri = Uri.parse("crunchyroll://media/$mediaId")

            StreamResolutionResult(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
}
