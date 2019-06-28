package me.proxer.app.anime.resolver

import android.content.pm.PackageManager
import io.reactivex.Single
import me.proxer.app.MainApplication.Companion.USER_AGENT
import me.proxer.app.exception.AppRequiredException
import me.proxer.app.exception.StreamResolutionException
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.androidUri
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.toBodySingle
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import org.koin.core.inject
import kotlin.text.RegexOption.DOT_MATCHES_ALL

/**
 * @author Ruben Gees
 */
object WatchboxStreamResolver : StreamResolver() {

    private const val WATCHBOX_PACKAGE = "com.rtli.clipfish"
    private val regex = Regex("\"al:android:url\" content=\"(.*?)?\"", DOT_MATCHES_ALL)

    override val name = "Watchbox"

    private val packageManager by inject<PackageManager>()

    override fun resolve(id: String): Single<StreamResolutionResult> = Single
        .fromCallable {
            if (!Utils.isPackageInstalled(packageManager, WATCHBOX_PACKAGE)) {
                throw AppRequiredException(name, WATCHBOX_PACKAGE)
            }
        }
        .flatMap { api.anime.link(id).buildSingle() }
        .flatMap { url ->
            client
                .newCall(
                    Request.Builder()
                        .get()
                        .url(Utils.parseAndFixUrl(url) ?: throw StreamResolutionException())
                        .header("User-Agent", USER_AGENT)
                        .header("Connection", "close")
                        .build()
                )
                .toBodySingle()
        }
        .map {
            regex.find(it)?.groupValues?.get(1)?.toHttpUrlOrNull()
                ?: throw StreamResolutionException()
        }
        .map { StreamResolutionResult.App(it.androidUri()) }
}
