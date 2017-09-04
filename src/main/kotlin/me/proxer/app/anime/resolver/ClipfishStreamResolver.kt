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

/**
 * @author Ruben Gees
 */
class ClipfishStreamResolver : StreamResolver() {

    private companion object {
        private const val CLIPFISH_PACKAGE = "com.rtli.clipfish"
        private val regex = Regex("video/(\\d+)?")
    }

    override val name = "Clipfish"

    override fun resolve(id: String): Single<StreamResolutionResult> = Single
            .fromCallable {
                if (!Utils.isPackageInstalled(globalContext.packageManager, CLIPFISH_PACKAGE)) {
                    throw AppRequiredException(name, CLIPFISH_PACKAGE)
                }
            }
            .flatMap { api.anime().link(id).buildSingle() }
            .flatMap { url ->
                client.newCall(Request.Builder()
                        .get()
                        .url(Utils.parseAndFixUrl(url))
                        .header("User-Agent", USER_AGENT)
                        .build())
                        .toBodySingle()
            }
            .map {
                val mediaId = regex.find(it)?.groupValues?.get(1) ?: throw StreamResolutionException()

                if (mediaId.isBlank()) {
                    throw StreamResolutionException()
                }

                StreamResolutionResult(Intent(Intent.ACTION_VIEW,
                        Uri.parse("clipfish://video/$id?ref=proxer")))
            }
}
