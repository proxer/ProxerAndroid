package me.proxer.app.anime.resolver

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import me.proxer.app.anime.stream.StreamActivity
import me.proxer.app.base.CustomTabsAware
import me.proxer.app.util.extension.addReferer
import me.proxer.app.util.extension.androidUri
import me.proxer.library.enums.AnimeLanguage
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
            const val ID_EXTRA = "id"
            const val NAME_EXTRA = "name"
            const val EPISODE_EXTRA = "episode"
            const val LANGUAGE_EXTRA = "language"
            const val COVER_EXTRA = "cover"
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
            id: String? = null,
            name: String? = null,
            episode: Int? = null,
            language: AnimeLanguage? = null,
            coverUri: Uri? = null,
            forceInternal: Boolean = false
        ): Intent {
            return intent
                .apply { if (forceInternal) component = ComponentName(context, StreamActivity::class.java) }
                .apply { if (id != null) putExtra(ID_EXTRA, id) }
                .apply { if (name != null) putExtra(NAME_EXTRA, name) }
                .apply { if (episode != null) putExtra(EPISODE_EXTRA, episode) }
                .apply { if (language != null) putExtra(LANGUAGE_EXTRA, language) }
                .apply { if (coverUri != null) putExtra(COVER_EXTRA, coverUri) }
        }

        fun play(
            context: Context,
            id: String?,
            name: String?,
            episode: Int?,
            language: AnimeLanguage? = null,
            coverUri: Uri? = null,
            forceInternal: Boolean = false
        ) {
            context.startActivity(makeIntent(context, id, name, episode, language, coverUri, forceInternal))
        }
    }

    class Link(private val url: HttpUrl) : StreamResolutionResult() {

        fun show(customTabsAware: CustomTabsAware) {
            customTabsAware.showPage(url, skipCheck = true)
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
