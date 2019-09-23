package me.proxer.app.anime.resolver

import android.content.pm.PackageManager
import io.reactivex.Single
import me.proxer.app.exception.AppRequiredException
import me.proxer.app.exception.StreamResolutionException
import me.proxer.app.util.extension.androidUri
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.isPackageInstalled
import me.proxer.app.util.extension.safeInject
import me.proxer.app.util.extension.toPrefixedUrlOrNull

/**
 * @author Ruben Gees
 */
object NetflixStreamResolver : StreamResolver() {

    private const val NETFLIX_PACKAGE = "com.netflix.mediaclient"

    override val name = "Netflix"

    private val packageManager by safeInject<PackageManager>()

    override fun resolve(id: String): Single<StreamResolutionResult> = Single
        .fromCallable {
            if (!packageManager.isPackageInstalled(NETFLIX_PACKAGE)) {
                throw AppRequiredException(name, NETFLIX_PACKAGE)
            }
        }
        .flatMap { api.anime.link(id).buildSingle() }
        .map { it.toPrefixedUrlOrNull() ?: throw StreamResolutionException() }
        .map { StreamResolutionResult.App(it.androidUri()) }
}
