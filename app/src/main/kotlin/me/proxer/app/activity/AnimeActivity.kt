package me.proxer.app.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.ShareCompat
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import com.mikepenz.iconics.utils.IconicsMenuInflatorUtil
import me.proxer.app.R
import me.proxer.app.activity.base.MainActivity
import me.proxer.app.fragment.anime.AnimeFragment
import me.proxer.app.util.extension.bindView
import me.proxer.app.util.extension.toEpisodeAppString
import me.proxer.library.enums.AnimeLanguage
import me.proxer.library.enums.Category
import me.proxer.library.util.ProxerUtils
import org.jetbrains.anko.intentFor

/**
 * @author Ruben Gees
 */
class AnimeActivity : MainActivity() {

    companion object {
        private const val ID_EXTRA = "id"
        private const val EPISODE_EXTRA = "episode"
        private const val LANGUAGE_EXTRA = "language"
        private const val NAME_EXTRA = "name"
        private const val EPISODE_AMOUNT_EXTRA = "episode_amount"

        fun navigateTo(context: Activity, id: String, episode: Int, language: AnimeLanguage,
                       name: String? = null, episodeAmount: Int? = null) {
            context.startActivity(context.intentFor<AnimeActivity>(
                    ID_EXTRA to id,
                    EPISODE_EXTRA to episode,
                    LANGUAGE_EXTRA to language,
                    NAME_EXTRA to name,
                    EPISODE_AMOUNT_EXTRA to episodeAmount
            ))
        }
    }

    val id: String
        get() = when {
            intent.action == Intent.ACTION_VIEW -> intent.data.pathSegments.getOrElse(1, { "-1" })
            else -> intent.getStringExtra(ID_EXTRA)
        }

    var episode: Int
        get() = when {
            intent.action == Intent.ACTION_VIEW && !intent.hasExtra(EPISODE_EXTRA) -> {
                intent.data.pathSegments.getOrElse(2, { "1" }).toIntOrNull() ?: 1
            }
            else -> intent.getIntExtra(EPISODE_EXTRA, 1)
        }
        set(value) {
            intent.putExtra(EPISODE_EXTRA, value)

            updateTitle()
        }

    val language: AnimeLanguage
        get() = when {
            intent.action == Intent.ACTION_VIEW -> {
                ProxerUtils.toApiEnum(AnimeLanguage::class.java, intent.data.pathSegments.getOrElse(3, { "" }))
                        ?: AnimeLanguage.ENGLISH_SUB
            }
            else -> intent.getSerializableExtra(LANGUAGE_EXTRA) as AnimeLanguage
        }

    var name: String?
        get() = intent.getStringExtra(NAME_EXTRA)
        set(value) {
            intent.putExtra(NAME_EXTRA, value)

            updateTitle()
        }

    var episodeAmount: Int?
        get() {
            if (intent.hasExtra(EPISODE_AMOUNT_EXTRA)) {
                return intent.getIntExtra(EPISODE_AMOUNT_EXTRA, 1)
            } else {
                return null
            }
        }
        set(value) {
            if (value == null) {
                intent.extras.remove(EPISODE_AMOUNT_EXTRA)
            } else {
                intent.putExtra(EPISODE_AMOUNT_EXTRA, value)
            }
        }

    private val toolbar: Toolbar by bindView(R.id.toolbar)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_default)
        setSupportActionBar(toolbar)

        setupToolbar()
        updateTitle()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .setAllowOptimization(true)
                    .replace(R.id.container, AnimeFragment.newInstance())
                    .commitNow()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        IconicsMenuInflatorUtil.inflate(menuInflater, this, R.menu.activity_share, menu, true)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_share -> {
                ShareCompat.IntentBuilder
                        .from(this)
                        .setText("https://proxer.me/watch/$id/$episode/${ProxerUtils.getApiEnumName(language)}")
                        .setType("text/plain")
                        .setChooserTitle(getString(R.string.share_title))
                        .startChooser()

                return true
            }
            android.R.id.home -> {
                finish()

                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setOnClickListener {
            name?.let {
                MediaActivity.navigateTo(this, id, it, Category.ANIME)
            }
        }
    }

    private fun updateTitle() {
        title = Category.ANIME.toEpisodeAppString(this, episode)
        supportActionBar?.subtitle = name
    }
}
