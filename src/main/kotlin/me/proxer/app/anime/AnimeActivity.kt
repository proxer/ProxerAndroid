package me.proxer.app.anime

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.core.app.ShareCompat
import androidx.fragment.app.commitNow
import com.jakewharton.rxbinding3.view.clicks
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import me.proxer.app.R
import me.proxer.app.base.DrawerActivity
import me.proxer.app.media.MediaActivity
import me.proxer.app.util.extension.startActivity
import me.proxer.app.util.extension.toEpisodeAppString
import me.proxer.library.enums.AnimeLanguage
import me.proxer.library.enums.Category
import me.proxer.library.util.ProxerUrls
import me.proxer.library.util.ProxerUtils

/**
 * @author Ruben Gees
 */
class AnimeActivity : DrawerActivity() {

    companion object {
        private const val ID_EXTRA = "id"
        private const val EPISODE_EXTRA = "episode"
        private const val LANGUAGE_EXTRA = "language"
        private const val NAME_EXTRA = "name"
        private const val EPISODE_AMOUNT_EXTRA = "episode_amount"

        fun navigateTo(
            context: Activity,
            id: String,
            episode: Int,
            language: AnimeLanguage,
            name: String? = null,
            episodeAmount: Int? = null
        ) {
            context.startActivity<AnimeActivity>(
                ID_EXTRA to id,
                EPISODE_EXTRA to episode,
                LANGUAGE_EXTRA to language,
                NAME_EXTRA to name,
                EPISODE_AMOUNT_EXTRA to episodeAmount
            )
        }
    }

    val id: String
        get() = when (intent.action) {
            Intent.ACTION_VIEW -> intent?.data?.pathSegments?.getOrElse(1) { "-1" } ?: "-1"
            else -> intent.getStringExtra(ID_EXTRA)
        }

    var episode: Int
        get() = when {
            intent.action == Intent.ACTION_VIEW && !intent.hasExtra(EPISODE_EXTRA) -> intent?.data?.pathSegments
                ?.getOrElse(2) { "1" }?.toIntOrNull() ?: 1
            else -> intent.getIntExtra(EPISODE_EXTRA, 1)
        }
        set(value) {
            intent.putExtra(EPISODE_EXTRA, value)

            updateTitle()
        }

    val language: AnimeLanguage
        get() = when (intent.action) {
            Intent.ACTION_VIEW -> ProxerUtils.toApiEnum<AnimeLanguage>(
                intent?.data?.pathSegments
                    ?.getOrElse(3) { "" } ?: "") ?: AnimeLanguage.ENGLISH_SUB
            else -> intent.getSerializableExtra(LANGUAGE_EXTRA) as AnimeLanguage
        }

    var name: String?
        get() = intent.getStringExtra(NAME_EXTRA)
        set(value) {
            intent.putExtra(NAME_EXTRA, value)

            updateTitle()
        }

    var episodeAmount: Int?
        get() = when (intent.hasExtra(EPISODE_AMOUNT_EXTRA)) {
            true -> intent.getIntExtra(EPISODE_AMOUNT_EXTRA, 1)
            false -> null
        }
        set(value) {
            if (value == null) {
                intent.removeExtra(EPISODE_AMOUNT_EXTRA)
            } else {
                intent.putExtra(EPISODE_AMOUNT_EXTRA, value)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupToolbar()
        updateTitle()

        if (savedInstanceState == null) {
            supportFragmentManager.commitNow {
                replace(R.id.container, AnimeFragment.newInstance())
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        IconicsMenuInflaterUtil.inflate(menuInflater, this, R.menu.activity_share, menu, true)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_share -> name?.let {
                ShareCompat.IntentBuilder
                    .from(this)
                    .setText(getString(R.string.share_anime, episode, it, ProxerUrls.animeWeb(id, episode, language)))
                    .setType("text/plain")
                    .setChooserTitle(getString(R.string.share_title))
                    .startChooser()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.clicks()
            .autoDisposable(this.scope())
            .subscribe {
                name?.let {
                    MediaActivity.navigateTo(this, id, it, Category.ANIME)
                }
            }
    }

    private fun updateTitle() {
        title = name
        supportActionBar?.subtitle = Category.ANIME.toEpisodeAppString(this, episode)
    }
}
