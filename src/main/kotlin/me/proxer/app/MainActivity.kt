package me.proxer.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.postDelayed
import androidx.fragment.app.Fragment
import androidx.fragment.app.commitNow
import com.google.android.material.tabs.TabLayout
import com.rubengees.introduction.IntroductionActivity.OPTION_RESULT
import com.rubengees.introduction.IntroductionBuilder
import com.rubengees.introduction.Option
import kotterknife.bindView
import me.proxer.app.anime.schedule.ScheduleFragment
import me.proxer.app.base.BackPressAware
import me.proxer.app.base.DrawerActivity
import me.proxer.app.bookmark.BookmarkFragment
import me.proxer.app.chat.ChatContainerFragment
import me.proxer.app.media.list.MediaListFragment
import me.proxer.app.news.NewsFragment
import me.proxer.app.notification.NotificationWorker
import me.proxer.app.profile.settings.ProfileSettingsViewModel
import me.proxer.app.settings.AboutFragment
import me.proxer.app.settings.SettingsFragment
import me.proxer.app.settings.theme.Theme
import me.proxer.app.settings.theme.ThemeContainer
import me.proxer.app.settings.theme.ThemeVariant
import me.proxer.app.ui.view.RatingDialog
import me.proxer.app.util.InAppUpdateFlow
import me.proxer.app.util.extension.intentFor
import me.proxer.app.util.wrapper.IntroductionWrapper
import me.proxer.app.util.wrapper.MaterialDrawerWrapper.DrawerItem
import me.proxer.library.enums.Category
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.threeten.bp.Instant
import org.threeten.bp.temporal.ChronoUnit

/**
 * @author Ruben Gees
 */
class MainActivity : DrawerActivity() {

    companion object {
        private const val TITLE_STATE = "title"
        private const val SECTION_EXTRA = "section"
        private const val SECTION_ACTION_PREFIX = "me.proxer.app.intent.action."

        fun navigateToSection(context: Context, section: DrawerItem) = context
            .startActivity(getSectionIntent(context, section))

        fun getSectionIntent(context: Context, section: DrawerItem): Intent = context
            .intentFor<MainActivity>(SECTION_EXTRA to section)
            .setAction(SECTION_ACTION_PREFIX + section.name)
    }

    override val contentView = R.layout.activity_main

    override val isRootActivity get() = intent.action != Intent.ACTION_VIEW && !intent.hasExtra(SECTION_EXTRA)
    override val isMainActivity = true

    val tabs: TabLayout by bindView(R.id.tabs)

    private val ucpSettingsViewModel by viewModel<ProfileSettingsViewModel>()

    private val inAppUpdateFlow = InAppUpdateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportPostponeEnterTransition()
        displayFirstPage(savedInstanceState)

        if (!isRootActivity) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        if (isRootActivity && savedInstanceState == null && storageHelper.isLoggedIn) {
            val lastUcpSettingsUpdate = storageHelper.lastUcpSettingsUpdateDate
            val threshold = Instant.now().minus(5, ChronoUnit.MINUTES)

            if (threshold.isAfter(lastUcpSettingsUpdate)) {
                ucpSettingsViewModel.refresh()
            }
        }

        root.postDelayed(50) {
            supportStartPostponedEnterTransition()
        }

        if (intent.action == Intent.ACTION_MAIN && savedInstanceState == null) {
            preferenceHelper.incrementLaunches()

            preferenceHelper.launches.let { launches ->
                if (launches >= 3 && launches % 3 == 0 && !preferenceHelper.hasRated) {
                    RatingDialog.show(this)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putCharSequence(TITLE_STATE, title)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        title = savedInstanceState.getString(TITLE_STATE)
    }

    override fun onDestroy() {
        inAppUpdateFlow.stop()

        super.onDestroy()
    }

    override fun onBackPressed() {
        val fragmentList = supportFragmentManager.fragments

        if (!drawer.onBackPressed() && fragmentList.none { it is BackPressAware && it.onBackPressed() }) {
            if (isRootActivity) {
                val startPage = preferenceHelper.startPage

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == IntroductionBuilder.INTRODUCTION_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                data?.getParcelableArrayListExtra<Option>(OPTION_RESULT)?.forEach { option ->
                    when (option.position) {
                        1 -> {
                            preferenceHelper.areNewsNotificationsEnabled = option.isActivated
                            preferenceHelper.areAccountNotificationsEnabled = option.isActivated

                            NotificationWorker.enqueueIfPossible(delay = true)
                        }
                        2 -> if (option.isActivated) {
                            preferenceHelper.themeContainer = ThemeContainer(Theme.CLASSIC, ThemeVariant.DARK)
                        }
                    }
                }
            }

            displayFirstPage(null)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (intent.action != Intent.ACTION_MAIN) {
            this.intent = intent
        }

        if (intent.hasExtra(SECTION_EXTRA)) {
            val itemToLoad = getItemToLoad()

            drawer.select(itemToLoad, false)

            setFragment(itemToLoad)
        }
    }

    private fun setFragment(item: DrawerItem) {
        when (item) {
            DrawerItem.NEWS -> setFragment(NewsFragment.newInstance(), R.string.section_news)
            DrawerItem.CHAT -> setFragment(ChatContainerFragment.newInstance(), R.string.section_chat)
            DrawerItem.MESSENGER -> setFragment(ChatContainerFragment.newInstance(true), R.string.section_chat)
            DrawerItem.BOOKMARKS -> setFragment(BookmarkFragment.newInstance(), R.string.section_bookmarks)
            DrawerItem.ANIME -> setFragment(MediaListFragment.newInstance(Category.ANIME), R.string.section_anime)
            DrawerItem.SCHEDULE -> setFragment(ScheduleFragment.newInstance(), R.string.section_schedule)
            DrawerItem.MANGA -> setFragment(MediaListFragment.newInstance(Category.MANGA), R.string.section_manga)
            DrawerItem.INFO -> setFragment(AboutFragment.newInstance(), R.string.section_info)
            DrawerItem.SETTINGS -> setFragment(SettingsFragment.newInstance(), R.string.section_settings)
        }
    }

    private fun setFragment(fragment: Fragment, newTitle: Int) {
        title = getString(newTitle)

        supportFragmentManager.commitNow {
            replace(R.id.container, fragment)
        }
    }

    private fun displayFirstPage(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            val shouldIntroduce = preferenceHelper.launches <= 0 && intent.action == Intent.ACTION_MAIN

            if (shouldIntroduce) {
                IntroductionWrapper.introduce(this)
            } else {
                val itemToLoad = getItemToLoad()

                drawer.select(itemToLoad, false)

                setFragment(itemToLoad)

                if (!isRootActivity) {
                    drawer.disableSelectivity()
                }

                if (isRootActivity) {
                    inAppUpdateFlow.start(this, root)
                }
            }
        }
    }

    private fun getItemToLoad(): DrawerItem {
        return when (val actionDrawerItem = when (intent.action == Intent.ACTION_VIEW) {
            true -> when (intent.data?.pathSegments?.firstOrNull()) {
                "news" -> DrawerItem.NEWS
                "chat" -> DrawerItem.CHAT
                "messages" -> DrawerItem.MESSENGER
                "reminder" -> DrawerItem.BOOKMARKS
                "anime" -> DrawerItem.ANIME
                "calendar" -> DrawerItem.SCHEDULE
                "manga" -> DrawerItem.MANGA
                else -> null
            }
            false -> null
        }) {
            null -> {
                val sectionExtra = intent.getSerializableExtra(SECTION_EXTRA) as? DrawerItem

                sectionExtra ?: preferenceHelper.startPage
            }
            else -> actionDrawerItem
        }
    }

    override fun handleDrawerItemClick(item: DrawerItem) = when (isRootActivity || item == drawer.currentItem) {
        true -> setFragment(item)
        false -> super.handleDrawerItemClick(item)
    }
}
