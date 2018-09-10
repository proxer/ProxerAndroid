package me.proxer.app.anime.resolver

import android.content.Intent
import android.content.pm.PackageManager
import io.reactivex.Single
import me.proxer.app.exception.AppRequiredException
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.androidUri
import me.proxer.app.util.extension.buildSingle
import org.koin.standalone.inject

/**
 * @author Ruben Gees
 */
class ViewsterStreamResolver : StreamResolver() {

    companion object {
        private const val VIEWSTER_PACKAGE = "com.viewster.androidapp"
    }

    override val name = "Viewster"

    private val packageManager by inject<PackageManager>()

    override fun resolve(id: String): Single<StreamResolutionResult> = Single
        .fromCallable {
            if (!Utils.isPackageInstalled(packageManager, VIEWSTER_PACKAGE)) {
                throw AppRequiredException(name, VIEWSTER_PACKAGE)
            }
        }
        .flatMap { api.anime().link(id).buildSingle() }
        .map {
            val uri = Utils.getAndFixUrl(it.replace("/embed", "")).androidUri()

            StreamResolutionResult(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
}
