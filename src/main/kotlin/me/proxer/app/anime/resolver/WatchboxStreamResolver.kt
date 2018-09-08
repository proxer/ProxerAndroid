package me.proxer.app.anime.resolver

import android.content.Intent
import android.net.Uri
import io.reactivex.Single
import me.proxer.app.MainApplication.Companion.USER_AGENT
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.MainApplication.Companion.client
import me.proxer.app.MainApplication.Companion.globalContext
import me.proxer.app.exception.AppRequiredException
import me.proxer.app.exception.StreamResolutionException
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.toBodySingle
import okhttp3.Request
import kotlin.text.RegexOption.DOT_MATCHES_ALL

/**
 * @author Ruben Gees
 */
class WatchboxStreamResolver : StreamResolver {

    private companion object {
        private const val WATCHBOX_PACKAGE = "com.rtli.clipfish"
        private val regex = Regex("\"al:android:url\" content=\"(.*?)?\"", DOT_MATCHES_ALL)
    }

    override val name = "Watchbox"

    override fun resolve(id: String): Single<StreamResolutionResult> = Single
        .fromCallable {
            if (!Utils.isPackageInstalled(globalContext.packageManager, WATCHBOX_PACKAGE)) {
                throw AppRequiredException(name, WATCHBOX_PACKAGE)
            }
        }
        .flatMap { api.anime().link(id).buildSingle() }
        .flatMap { url ->
            client.newCall(
                Request.Builder()
                    .get()
                    .url(Utils.getAndFixUrl(url))
                    .header("User-Agent", USER_AGENT)
                    .build()
            )
                .toBodySingle()
        }
        .map {
            val mediaUri = regex.find(it)?.groupValues?.get(1) ?: throw StreamResolutionException()

            if (mediaUri.isBlank()) {
                throw StreamResolutionException()
            }

            val uri = Uri.parse(mediaUri)

            StreamResolutionResult(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
}
