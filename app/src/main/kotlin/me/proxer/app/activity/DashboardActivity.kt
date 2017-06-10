package me.proxer.app.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.view.ViewCompat
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.ImageView
import com.rubengees.introduction.IntroductionActivity.OPTION_RESULT
import com.rubengees.introduction.IntroductionBuilder
import com.rubengees.introduction.Option
import me.proxer.app.R
import me.proxer.app.activity.base.MainActivity
import me.proxer.app.dialog.LoginDialog
import me.proxer.app.dialog.LogoutDialog
import me.proxer.app.event.LoginEvent
import me.proxer.app.event.LogoutEvent
import me.proxer.app.fragment.AboutFragment
import me.proxer.app.fragment.SettingsFragment
import me.proxer.app.fragment.chat.ConferencesFragment
import me.proxer.app.fragment.manga.LocalMangaFragment
import me.proxer.app.fragment.media.MediaListFragment
import me.proxer.app.fragment.news.NewsArticleFragment
import me.proxer.app.fragment.ucp.BookmarksFragment
import me.proxer.app.helper.IntroductionHelper
import me.proxer.app.helper.MaterialDrawerHelper
import me.proxer.app.helper.MaterialDrawerHelper.AccountItem
import me.proxer.app.helper.MaterialDrawerHelper.DrawerItem
import me.proxer.app.helper.PreferenceHelper
import me.proxer.app.helper.StorageHelper
import me.proxer.app.job.NotificationsJob
import me.proxer.app.util.extension.bindView
import me.proxer.library.enums.Category
import me.proxer.library.enums.Device
import me.proxer.library.util.ProxerUrls
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.intentFor

/**
 * @author Ruben Gees
 */
class DashboardActivity : MainActivity() {

    companion object {
        private const val TITLE_STATE = "title"
        private const val SECTION_EXTRA = "section"

        fun getSectionIntent(context: Context, section: DrawerItem): Intent {
            return context.intentFor<DashboardActivity>(SECTION_EXTRA to section.id)
        }
    }

    private lateinit var drawer: MaterialDrawerHelper

    private val toolbar: Toolbar by bindView(R.id.toolbar)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_default)
        setSupportActionBar(toolbar)

        drawer = MaterialDrawerHelper(this, toolbar, savedInstanceState,
                { onDrawerItemClick(it) }, { view, item -> onAccountItemClick(view, item) })

        displayFirstPage(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()

        drawer.refreshHeader(this)
    }

    override fun onStart() {
        super.onStart()

        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)

        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putCharSequence(TITLE_STATE, title)
        drawer.saveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)

        if (savedInstanceState != null) {
            title = savedInstanceState.getString(TITLE_STATE)
        }
    }

    override fun onBackPressed() {
        if (!drawer.onBackPressed()) {
            super.onBackPressed()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == IntroductionBuilder.INTRODUCTION_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                data.getParcelableArrayListExtra<Option>(OPTION_RESULT).forEach { option ->
                    when (option.position) {
                        1 -> {
                            doAsync {
                                PreferenceHelper.setNotificationsEnabled(this@DashboardActivity, option.isActivated)
                                NotificationsJob.scheduleIfPossible(this@DashboardActivity)
                            }
                        }
                    }
                }
            }

            StorageHelper.isFirstStart = false
            displayFirstPage(null)
        }
    }

    private fun setFragment(fragment: Fragment, newTitle: Int) {
        title = getString(newTitle)

        setFragment(fragment)
    }

    private fun setFragment(fragment: Fragment): Unit {
        supportFragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .commitNow()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUserChanged(@Suppress("UNUSED_PARAMETER") event: LoginEvent) {
        drawer.refreshHeader(this)
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLogout(@Suppress("UNUSED_PARAMETER") event: LogoutEvent) {
        drawer.refreshHeader(this)
    }

    private fun displayFirstPage(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            if (StorageHelper.isFirstStart) {
                IntroductionHelper.introduce(this)
            } else {
                drawer.select(getItemToLoad())
            }
        }
    }

    private fun getItemToLoad() = DrawerItem.fromOrDefault(when (intent.action == Intent.ACTION_VIEW) {
        true -> intent.dataString.toLongOrNull()
        false -> intent.getLongExtra(SECTION_EXTRA, PreferenceHelper.getStartPage(this).id)
    })

    private fun onDrawerItemClick(item: DrawerItem): Boolean {
        when (item) {
            DrawerItem.NEWS -> {
                setFragment(NewsArticleFragment.newInstance(), R.string.section_news)

                return false
            }

            DrawerItem.CHAT -> {
                setFragment(ConferencesFragment.newInstance(), R.string.section_chat)

                return false
            }

            DrawerItem.BOOKMARKS -> {
                setFragment(BookmarksFragment.newInstance(), R.string.section_bookmarks)

                return false
            }

            DrawerItem.ANIME -> {
                setFragment(MediaListFragment.newInstance(Category.ANIME), R.string.section_anime)

                return false
            }

            DrawerItem.MANGA -> {
                setFragment(MediaListFragment.newInstance(Category.MANGA), R.string.section_manga)

                return false
            }

            DrawerItem.LOCAL_MANGA -> {
                setFragment(LocalMangaFragment.newInstance(), R.string.section_local_manga)

                return false
            }

            DrawerItem.INFO -> {
                setFragment(AboutFragment.newInstance(), R.string.section_info)

                return false
            }

            DrawerItem.DONATE -> {
                showPage(ProxerUrls.donateWeb(Device.DEFAULT))

                return true
            }

            DrawerItem.SETTINGS -> {
                setFragment(SettingsFragment.newInstance(), R.string.section_settings)

                return false
            }

            else -> return false
        }
    }

    private fun onAccountItemClick(view: View, item: AccountItem): Boolean {
        when (item) {
            AccountItem.GUEST -> {
                LoginDialog.show(this)

                return false
            }

            AccountItem.LOGIN -> {
                LoginDialog.show(this)

                return false
            }

            AccountItem.LOGOUT -> {
                LogoutDialog.show(this)

                return false
            }

            AccountItem.USER -> {
                StorageHelper.user?.let {
                    val viewToUse = when (view) {
                        is ImageView -> view.apply { ViewCompat.setTransitionName(this, "profile_image") }
                        else -> null
                    }

                    ProfileActivity.navigateTo(this, it.id, it.name, it.image, viewToUse)

                    return viewToUse != null
                }

                return false
            }

            AccountItem.UCP -> {
                UcpActivity.navigateTo(this)

                return false
            }
        }
    }
}
