package me.proxer.app

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import com.rubengees.introduction.IntroductionActivity.OPTION_RESULT
import com.rubengees.introduction.IntroductionBuilder
import com.rubengees.introduction.Option
import me.proxer.app.anime.ScheduleFragment
import me.proxer.app.base.BackPressAware
import me.proxer.app.base.DrawerActivity
import me.proxer.app.bookmark.BookmarkFragment
import me.proxer.app.chat.conference.ConferenceFragment
import me.proxer.app.media.list.MediaListFragment
import me.proxer.app.news.NewsFragment
import me.proxer.app.notification.NotificationJob
import me.proxer.app.settings.AboutFragment
import me.proxer.app.settings.SettingsFragment
import me.proxer.app.ui.view.RatingDialog
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.extension.shortcutManager
import me.proxer.app.util.wrapper.IntroductionWrapper
import me.proxer.app.util.wrapper.MaterialDrawerWrapper.DrawerItem
import me.proxer.library.enums.Category
import me.proxer.library.enums.Device
import me.proxer.library.util.ProxerUrls
import org.jetbrains.anko.intentFor

/**
 * @author Ruben Gees
 */
class MainActivity : DrawerActivity() {

    companion object {
        private const val TITLE_STATE = "title"
        private const val SECTION_EXTRA = "section"

        private const val SHORTCUT_NEWS = "news"
        private const val SHORTCUT_CHAT = "chat"
        private const val SHORTCUT_BOOKMARKS = "bookmarks"

        fun navigateToSection(context: Context, section: DrawerItem) = context
            .startActivity(getSectionIntent(context, section))

        fun getSectionIntent(context: Context, section: DrawerItem) = context
            .intentFor<MainActivity>(SECTION_EXTRA to section.id)
    }

    override val isRootActivity get() = !intent.hasExtra(SECTION_EXTRA)
    override val isMainActivity = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportPostponeEnterTransition()
        displayFirstPage(savedInstanceState)

        if (!isRootActivity) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        root.postDelayed({
            supportStartPostponedEnterTransition()
        }, 50)

        if (intent.action != Intent.ACTION_VIEW && !intent.hasExtra(SECTION_EXTRA)) {
            PreferenceHelper.incrementLaunches(this)

            PreferenceHelper.getLaunches(this).let { launches ->
                if (launches >= 3 && launches % 3 == 0 && !PreferenceHelper.hasRated(this)) {
                    RatingDialog.show(this)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putCharSequence(TITLE_STATE, title)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)

        if (savedInstanceState != null) {
            title = savedInstanceState.getString(TITLE_STATE)
        }
    }

    override fun onBackPressed() {
        val fragmentList = supportFragmentManager.fragments

        if (!drawer.onBackPressed() && fragmentList.none { it is BackPressAware && it.onBackPressed() }) {
            if (isRootActivity) {
                val startPage = PreferenceHelper.getStartPage(this)

                if (startPage != drawer.currentItem) {
                    drawer.select(startPage)
                } else {
                    super.onBackPressed()
                }
            } else {
                super.onBackPressed()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == IntroductionBuilder.INTRODUCTION_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                data.getParcelableArrayListExtra<Option>(OPTION_RESULT).forEach { option ->
                    when (option.position) {
                        1 -> {
                            PreferenceHelper.setNewsNotificationsEnabled(this, option.isActivated)
                            PreferenceHelper.setAccountNotificationsEnabled(this, option.isActivated)

                            NotificationJob.scheduleIfPossible(this)
                        }
                        2 -> PreferenceHelper.setVerticalReaderEnabled(this, option.isActivated)
                    }
                }
            }

            displayFirstPage(null)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        this.intent = intent

        if (intent.hasExtra(SECTION_EXTRA)) {
            drawer.select(DrawerItem.fromIdOrDefault(intent.getLongExtra(SECTION_EXTRA, -1)))
        }
    }

    private fun setFragment(item: DrawerItem) {
        when (item) {
            DrawerItem.NEWS -> setFragment(NewsFragment.newInstance(), R.string.section_news)
            DrawerItem.CHAT -> setFragment(ConferenceFragment.newInstance(), R.string.section_chat)
            DrawerItem.BOOKMARKS -> setFragment(BookmarkFragment.newInstance(), R.string.section_bookmarks)
            DrawerItem.ANIME -> setFragment(MediaListFragment.newInstance(Category.ANIME), R.string.section_anime)
            DrawerItem.SCHEDULE -> setFragment(ScheduleFragment.newInstance(), R.string.section_schedule)
            DrawerItem.MANGA -> setFragment(MediaListFragment.newInstance(Category.MANGA), R.string.section_manga)
            DrawerItem.INFO -> setFragment(AboutFragment.newInstance(), R.string.section_info)
            DrawerItem.DONATE -> showPage(ProxerUrls.donateWeb(Device.DEFAULT))
            DrawerItem.SETTINGS -> setFragment(SettingsFragment.newInstance(), R.string.section_settings)
        }
    }

    private fun setFragment(fragment: Fragment, newTitle: Int) {
        title = getString(newTitle)

        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commitNow()
    }

    private fun displayFirstPage(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            val shouldIntroduce = PreferenceHelper.getLaunches(this) <= 0 &&
                intent.action != Intent.ACTION_VIEW && !intent.hasExtra(SECTION_EXTRA)

            if (shouldIntroduce) {
                IntroductionWrapper.introduce(this)
            } else {
                val itemToLoad = getItemToLoad()

                drawer.select(itemToLoad, isRootActivity)

                if (!isRootActivity) {
                    setFragment(itemToLoad)

                    drawer.disableSelectability()
                }
            }
        }
    }

    private fun getItemToLoad() = DrawerItem.fromIdOrDefault(when (intent.action == Intent.ACTION_VIEW) {
        true -> intent.dataString.toLongOrNull().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                shortcutManager.reportShortcutUsed(when (this) {
                    0L -> SHORTCUT_NEWS
                    1L -> SHORTCUT_CHAT
                    2L -> SHORTCUT_BOOKMARKS
                    else -> null
                })
            }
        }
        false -> intent.getLongExtra(SECTION_EXTRA, PreferenceHelper.getStartPage(this).id)
    })

    override fun handleDrawerItemClick(item: DrawerItem) = when (isRootActivity || item == drawer.currentItem) {
        true -> setFragment(item)
        false -> super.handleDrawerItemClick(item)
    }
}
