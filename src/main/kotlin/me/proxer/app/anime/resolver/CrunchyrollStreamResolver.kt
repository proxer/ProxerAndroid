package me.proxer.app.anime.resolver

import android.content.pm.PackageManager
import android.net.Uri
import io.reactivex.Single
import me.proxer.app.exception.AppRequiredException
import me.proxer.app.exception.StreamResolutionException
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.isPackageInstalled
import me.proxer.app.util.extension.safeInject

/**
 * @author Ruben Gees
 */
object CrunchyrollStreamResolver : StreamResolver() {

    private const val CRUNCHYROLL_PACKAGE = "com.crunchyroll.crunchyroid"
    private val regex = Regex("media_id=(\\d*)?")

    override val name = "Crunchyroll"

    private val packageManager by safeInject<PackageManager>()

    override fun supports(name: String) = name.startsWith(this.name, true)

    override fun resolve(id: String): Single<StreamResolutionResult> = Single
        .fromCallable {
            if (!packageManager.isPackageInstalled(CRUNCHYROLL_PACKAGE)) {
                throw AppRequiredException(name, CRUNCHYROLL_PACKAGE)
            }
        }
        .flatMap { api.anime.link(id).buildSingle() }
        .map { url ->
            val regexResult = regex.find(url) ?: throw StreamResolutionException()
            val mediaId = regexResult.groupValues[1]

            if (mediaId.isBlank()) {
                throw StreamResolutionException()
            }

            StreamResolutionResult.App(Uri.parse("crunchyroll://media/${Uri.encode(mediaId)}"))
        }
}
