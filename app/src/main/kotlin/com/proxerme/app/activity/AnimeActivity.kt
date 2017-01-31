package com.proxerme.app.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.ShareCompat
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import com.proxerme.app.R
import com.proxerme.app.entitiy.EntryInfo
import com.proxerme.app.fragment.anime.AnimeFragment
import com.proxerme.app.util.bindView
import com.proxerme.library.parameters.SubDubLanguageParameter
import org.jetbrains.anko.startActivity

class AnimeActivity : MainActivity() {

    companion object {
        private const val EXTRA_ID = "extra_id"
        private const val EXTRA_EPISODE = "extra_episode"
        private const val EXTRA_LANGUAGE = "extra_language"
        private const val EXTRA_ENTRY_INFO = "extra_entryInfo"

        fun navigateTo(context: Activity, id: String, episode: Int, language: String,
                       name: String? = null, totalEpisodes: Int? = null) {
            context.startActivity<AnimeActivity>(
                    EXTRA_ID to id,
                    EXTRA_EPISODE to episode,
                    EXTRA_LANGUAGE to language,
                    EXTRA_ENTRY_INFO to EntryInfo(name, totalEpisodes)
            )
        }
    }

    val id: String
        get() = when {
            intent.action == Intent.ACTION_VIEW -> intent.data.pathSegments.getOrElse(1, { "-1" })
            else -> intent.getStringExtra(EXTRA_ID)
        }

    var episode: Int
        get() = when {
            intent.action == Intent.ACTION_VIEW && !intent.hasExtra(EXTRA_EPISODE) -> {
                intent.data.pathSegments.getOrElse(2, { "1" }).toIntOrNull() ?: 1
            }
            else -> intent.getIntExtra(EXTRA_EPISODE, 1)
        }
        set(value) {
            intent.putExtra(EXTRA_EPISODE, value)

            updateTitle()
        }

    val language: String
        get() = when {
            intent.action == Intent.ACTION_VIEW -> {
                intent.data.pathSegments.getOrElse(3, { SubDubLanguageParameter.ENGLISH_SUB })
            }
            else -> intent.getStringExtra(EXTRA_LANGUAGE)
        }

    var entryInfo: EntryInfo
        get() = intent.getParcelableExtra(EXTRA_ENTRY_INFO) ?: EntryInfo(null, null)
        set(value) {
            intent.putExtra(EXTRA_ENTRY_INFO, value)

            updateTitle()
        }

    private val toolbar: Toolbar by bindView(R.id.toolbar)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_default)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setOnClickListener {
            MediaActivity.navigateTo(this, id, entryInfo.name)
        }

        updateTitle()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .setAllowOptimization(true)
                    .replace(R.id.container, AnimeFragment.newInstance())
                    .commitNow()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_manga, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_share -> {
                ShareCompat.IntentBuilder
                        .from(this)
                        .setText("https://proxer.me/watch/$id/$episode/$language")
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

    private fun updateTitle() {
        title = getString(R.string.activity_anime_title, episode)
        supportActionBar?.subtitle = entryInfo.name
    }
}
