package me.proxer.app.anime.resolver

import android.content.pm.PackageManager
import android.net.Uri
import io.reactivex.Single
import me.proxer.app.exception.AppRequiredException
import me.proxer.app.exception.StreamResolutionException
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.buildSingle
import org.koin.core.inject

/**
 * @author Ruben Gees
 */
class CrunchyrollStreamResolver : StreamResolver() {

    private companion object {
        private const val CRUNCHYROLL_PACKAGE = "com.crunchyroll.crunchyroid"
        private val regex = Regex("media_id=(\\d*)?")
    }

    override val name = "Crunchyroll"

    private val packageManager by inject<PackageManager>()

    override fun supports(name: String) = name.startsWith(this.name, true)

    override fun resolve(id: String): Single<StreamResolutionResult> = Single
        .fromCallable {
            if (!Utils.isPackageInstalled(packageManager, CRUNCHYROLL_PACKAGE)) {
                throw AppRequiredException(name, CRUNCHYROLL_PACKAGE)
            }
        }
        .flatMap { api.anime.link(id).buildSingle() }
        .map { (link, _) ->
            val regexResult = regex.find(link) ?: throw StreamResolutionException()
            val mediaId = regexResult.groupValues[1]

            if (mediaId.isBlank()) {
                throw StreamResolutionException()
            }

            StreamResolutionResult.App(Uri.parse("crunchyroll://media/${Uri.encode(mediaId)}"))
        }
}
