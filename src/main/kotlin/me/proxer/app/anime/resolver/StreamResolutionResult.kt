package me.proxer.app.anime.resolver

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import me.proxer.app.anime.StreamActivity
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
        adTag: Uri? = null,
        internalPlayerOnly: Boolean = false
    ) : StreamResolutionResult() {

        companion object {
            const val NAME_EXTRA = "name"
            const val EPISODE_EXTRA = "episode"
            const val REFERER_EXTRA = "referer"
            const val AD_TAG_EXTRA = "ad_tag"
            const val INTERNAL_PLAYER_ONLY_EXTRA = "internal_player_only"
        }

        private val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(url.androidUri(), mimeType)
            .apply { if (referer != null) putExtra(REFERER_EXTRA, referer) }
            .putExtra(AD_TAG_EXTRA, adTag)
            .putExtra(INTERNAL_PLAYER_ONLY_EXTRA, internalPlayerOnly)
            .addReferer()

        fun makeIntent(
            context: Context,
            name: String? = null,
            episode: Int? = null,
            forceInternal: Boolean = false
        ): Intent {
            return intent
                .apply { if (forceInternal) component = ComponentName(context, StreamActivity::class.java) }
                .apply { if (name != null) putExtra(NAME_EXTRA, name) }
                .apply { if (episode != null) putExtra(EPISODE_EXTRA, episode) }
        }

        fun play(context: Context, name: String?, episode: Int?, forceInternal: Boolean = false) {
            context.startActivity(makeIntent(context, name, episode, forceInternal))
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
