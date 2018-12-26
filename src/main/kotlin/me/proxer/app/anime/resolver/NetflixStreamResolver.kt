package me.proxer.app.anime.resolver

import android.content.pm.PackageManager
import io.reactivex.Single
import me.proxer.app.exception.AppRequiredException
import me.proxer.app.exception.StreamResolutionException
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.androidUri
import me.proxer.app.util.extension.buildSingle
import org.koin.standalone.inject

/**
 * @author Ruben Gees
 */
class NetflixStreamResolver : StreamResolver() {

    private companion object {
        private const val NETFLIX_PACKAGE = "com.netflix.mediaclient"
    }

    override val name = "Netflix"

    private val packageManager by inject<PackageManager>()

    override fun resolve(id: String): Single<StreamResolutionResult> = Single
        .fromCallable {
            if (!Utils.isPackageInstalled(packageManager, NETFLIX_PACKAGE)) {
                throw AppRequiredException(name, NETFLIX_PACKAGE)
            }
        }
        .flatMap { api.anime().link(id).buildSingle() }
        .map { Utils.parseAndFixUrl(it) ?: throw StreamResolutionException() }
        .map { StreamResolutionResult.App(it.androidUri()) }
}
