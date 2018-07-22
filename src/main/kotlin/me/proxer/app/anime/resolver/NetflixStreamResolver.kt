package me.proxer.app.anime.resolver

import android.content.Intent
import android.net.Uri
import io.reactivex.Single
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.MainApplication.Companion.globalContext
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
    override val official = true

    override fun resolve(id: String): Single<StreamResolutionResult> = Single
        .fromCallable {
            if (!Utils.isPackageInstalled(globalContext.packageManager, NETFLIX_PACKAGE)) {
                throw AppRequiredException(name, NETFLIX_PACKAGE)
            }
        }
        .flatMap { api.anime().link(id).buildSingle() }
        .map {
            val uri = Uri.parse(it)

            StreamResolutionResult(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
}
