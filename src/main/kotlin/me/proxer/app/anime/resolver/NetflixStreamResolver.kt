package me.proxer.app.anime.resolver

import android.content.Intent
import android.net.Uri
import io.reactivex.Single
import me.proxer.app.MainApplication
import me.proxer.app.exception.AppRequiredException
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.buildSingle

/**
 * @author Ruben Gees
 */
class NetflixStreamResolver : StreamResolver {

    private companion object {
        private const val NETFLIX_PACKAGE = "com.netflix.mediaclient"
    }

    override val name = "Netflix"

    override fun resolve(id: String): Single<StreamResolutionResult> = Single
            .fromCallable {
                if (!Utils.isPackageInstalled(MainApplication.globalContext.packageManager, NETFLIX_PACKAGE)) {
                    throw AppRequiredException(name, NETFLIX_PACKAGE)
                }
            }
            .flatMap { MainApplication.api.anime().link(id).buildSingle() }
            .map { StreamResolutionResult(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
}
