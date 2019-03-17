package me.proxer.app.anime.resolver

import android.content.Context
import android.content.Intent
import android.net.Uri
import me.proxer.app.base.CustomTabsAware
import me.proxer.app.util.extension.addReferer
import me.proxer.app.util.extension.androidUri
import okhttp3.HttpUrl

/**
 * @author Ruben Gees
 */
sealed class StreamResolutionResult {

    class Video(
        url: HttpUrl,
        mimeType: String,
        referer: String? = null,
        adTag: Uri? = null
    ) : StreamResolutionResult() {

        companion object {
            const val NAME_EXTRA = "name"
            const val EPISODE_EXTRA = "episode"
            const val REFERER_EXTRA = "referer"
            const val AD_TAG_EXTRA = "ad_tag"
        }

        private val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(url.androidUri(), mimeType)
            .apply { if (referer != null) putExtra(REFERER_EXTRA, referer) }
            .putExtra(AD_TAG_EXTRA, adTag)
            .addReferer()

        fun play(context: Context, name: String?, episode: Int?) {
            context.startActivity(
                intent
                    .apply { if (name != null) putExtra(NAME_EXTRA, name) }
                    .apply { if (episode != null) putExtra(EPISODE_EXTRA, episode) }
            )
        }
    }

    class Link(private val url: HttpUrl) : StreamResolutionResult() {

        fun show(customTabsAware: CustomTabsAware) {
            customTabsAware.showPage(url)
        }
    }

    class App(uri: Uri) : StreamResolutionResult() {

        private val intent = Intent(Intent.ACTION_VIEW)
            .setData(uri)
            .addReferer()

        fun navigate(context: Context) {
            context.startActivity(intent)
        }
    }

    class Message(val message: CharSequence) : StreamResolutionResult()
}
