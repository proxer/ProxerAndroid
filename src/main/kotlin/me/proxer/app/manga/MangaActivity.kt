package me.proxer.app.manga

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v4.app.ShareCompat
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
import android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
import android.view.View.SYSTEM_UI_FLAG_IMMERSIVE
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
import android.view.View.SYSTEM_UI_FLAG_LOW_PROFILE
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.view.systemUiVisibilityChanges
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseActivity
import me.proxer.app.media.MediaActivity
import me.proxer.app.util.extension.autoDispose
import me.proxer.app.util.extension.toEpisodeAppString
import me.proxer.library.enums.Category
import me.proxer.library.enums.Language
import me.proxer.library.util.ProxerUtils
import org.jetbrains.anko.intentFor
import java.lang.ref.WeakReference

/**
 * @author Ruben Gees
 */
class MangaActivity : BaseActivity() {

    companion object {
        private const val ID_EXTRA = "id"
        private const val EPISODE_EXTRA = "episode"
        private const val LANGUAGE_EXTRA = "language"
        private const val CHAPTER_TITLE_EXTRA = "chapter_title"
        private const val NAME_EXTRA = "name"
        private const val EPISODE_AMOUNT_EXTRA = "episode_amount"

        fun navigateTo(
            context: Activity,
            id: String,
            episode: Int,
            language: Language,
            chapterTitle: String?,
            name: String? = null,
            episodeAmount: Int? = null
        ) {
            context.startActivity(context.intentFor<MangaActivity>(
                ID_EXTRA to id,
                EPISODE_EXTRA to episode,
                LANGUAGE_EXTRA to language,
                CHAPTER_TITLE_EXTRA to chapterTitle,
                NAME_EXTRA to name,
                EPISODE_AMOUNT_EXTRA to episodeAmount
            ))
        }
    }

    val id: String
        get() = when {
            intent.action == Intent.ACTION_VIEW -> intent.data.pathSegments.getOrElse(1) { "-1" }
            else -> intent.getStringExtra(ID_EXTRA)
        }

    var episode: Int
        get() = when {
            intent.action == Intent.ACTION_VIEW && !intent.hasExtra(EPISODE_EXTRA) -> intent.data.pathSegments
                .getOrElse(2) { "1" }.toIntOrNull() ?: 1
            else -> intent.getIntExtra(EPISODE_EXTRA, 1)
        }
        set(value) {
            intent.putExtra(EPISODE_EXTRA, value)

            updateTitle()
        }

    val language: Language
        get() = when {
            intent.action == Intent.ACTION_VIEW -> ProxerUtils.toApiEnum(Language::class.java, intent.data.pathSegments
                .getOrElse(3) { "" }) ?: Language.ENGLISH
            else -> intent.getSerializableExtra(LANGUAGE_EXTRA) as Language
        }

    var chapterTitle: String?
        get() = intent.getStringExtra(CHAPTER_TITLE_EXTRA)
        set(value) {
            intent.putExtra(CHAPTER_TITLE_EXTRA, value)

            updateTitle()
        }

    var name: String?
        get() = intent.getStringExtra(NAME_EXTRA)
        set(value) {
            intent.putExtra(NAME_EXTRA, value)

            updateTitle()
        }

    var episodeAmount: Int?
        get() = when {
            intent.hasExtra(EPISODE_AMOUNT_EXTRA) -> intent.getIntExtra(EPISODE_AMOUNT_EXTRA, 1)
            else -> null
        }
        set(value) {
            when (value) {
                null -> intent.extras.remove(EPISODE_AMOUNT_EXTRA)
                else -> intent.putExtra(EPISODE_AMOUNT_EXTRA, value)
            }
        }

    private val toolbar: Toolbar by bindView(R.id.toolbar)

    private val hideHandler = FullscreenHandler(WeakReference(this))
    private var isFullscreen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_manga)
        setSupportActionBar(toolbar)

        setupToolbar()
        updateTitle()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.decorView.systemUiVisibilityChanges()
                .autoDispose(this)
                .subscribe { visibility ->
                    if (visibility and SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                        window.decorView.systemUiVisibility = defaultUiFlags()

                        if (isFullscreen) {
                            hideHandler.removeMessages(0)
                            hideHandler.sendEmptyMessageDelayed(0, 2000)
                        }
                    }
                }
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MangaFragment.newInstance())
                .commitNow()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        IconicsMenuInflaterUtil.inflate(menuInflater, this, R.menu.activity_share, menu, true)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_share -> name?.let {
                val link = "https://proxer.me/chapter/$id/$episode/${ProxerUtils.getApiEnumName(language)}"
                val text = chapterTitle.let { title ->
                    when {
                        title.isNullOrBlank() -> getString(R.string.share_manga, episode, it, link)
                        else -> getString(R.string.share_manga_title, title, it, link)
                    }
                }

                ShareCompat.IntentBuilder
                    .from(this)
                    .setText(text)
                    .setType("text/plain")
                    .setChooserTitle(getString(R.string.share_title))
                    .startChooser()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    fun toggleFullscreen(fullscreen: Boolean) {
        isFullscreen = fullscreen

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (fullscreen) {
                window.decorView.systemUiVisibility = fullscreenUiFlags()
            } else {
                window.decorView.systemUiVisibility = defaultUiFlags()
            }
        }
    }

    private fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.clicks()
            .autoDispose(this)
            .subscribe {
                name?.let {
                    MediaActivity.navigateTo(this, id, name, Category.MANGA)
                }
            }
    }

    private fun updateTitle() {
        title = chapterTitle ?: Category.MANGA.toEpisodeAppString(this, episode)
        supportActionBar?.subtitle = name
    }

    private fun defaultUiFlags(): Int {
        return window.decorView.systemUiVisibility and
            SYSTEM_UI_FLAG_LOW_PROFILE.inv() and
            SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION.inv() and
            SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN.inv() and
            SYSTEM_UI_FLAG_HIDE_NAVIGATION.inv() and
            SYSTEM_UI_FLAG_FULLSCREEN.inv()
    }

    private fun fullscreenUiFlags(): Int {
        val result = SYSTEM_UI_FLAG_LOW_PROFILE or
            SYSTEM_UI_FLAG_LAYOUT_STABLE or
            SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            SYSTEM_UI_FLAG_FULLSCREEN

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return result or SYSTEM_UI_FLAG_IMMERSIVE
        } else {
            return result
        }
    }

    private class FullscreenHandler(private val activity: WeakReference<MangaActivity>) : Handler() {
        override fun handleMessage(msg: Message?) {
            activity.get()?.toggleFullscreen(true)
        }
    }
}
