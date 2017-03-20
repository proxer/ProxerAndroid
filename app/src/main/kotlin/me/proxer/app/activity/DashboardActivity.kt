package me.proxer.app.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.Toolbar
import com.rubengees.introduction.IntroductionActivity.OPTION_RESULT
import com.rubengees.introduction.IntroductionBuilder
import com.rubengees.introduction.entity.Option
import me.proxer.app.R
import me.proxer.app.dialog.LoginDialog
import me.proxer.app.dialog.LogoutDialog
import me.proxer.app.event.LogoutEvent
import me.proxer.app.event.UserChangedEvent
import me.proxer.app.fragment.AboutFragment
import me.proxer.app.fragment.SettingsFragment
import me.proxer.app.fragment.media.MediaListFragment
import me.proxer.app.fragment.news.NewsArticleFragment
import me.proxer.app.helper.IntroductionHelper
import me.proxer.app.helper.MaterialDrawerHelper
import me.proxer.app.helper.MaterialDrawerHelper.AccountItem
import me.proxer.app.helper.MaterialDrawerHelper.DrawerItem
import me.proxer.app.helper.PreferenceHelper
import me.proxer.app.helper.StorageHelper
import me.proxer.app.util.extension.bindView
import me.proxer.library.enums.Category
import me.proxer.library.enums.Device
import me.proxer.library.util.ProxerUrls
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.intentFor

/**
 * @author Ruben Gees
 */
class DashboardActivity : MainActivity() {

    companion object {
        private const val STATE_TITLE = "activity_dashboard_title"
        private const val EXTRA_DRAWER_ITEM = "extra_drawer_item"

        fun getSectionIntent(context: Context, item: DrawerItem): Intent {
            return context.intentFor<DashboardActivity>(EXTRA_DRAWER_ITEM to item.id)
        }
    }

    private lateinit var drawer: MaterialDrawerHelper

    private val toolbar: Toolbar by bindView(R.id.toolbar)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_default)
        setSupportActionBar(toolbar)

        drawer = MaterialDrawerHelper(this, toolbar, savedInstanceState,
                { onDrawerItemClick(it) }, { onAccountItemClick(it) })

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

        outState.putCharSequence(STATE_TITLE, title)
        drawer.saveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)

        if (savedInstanceState != null) {
            title = savedInstanceState.getString(STATE_TITLE)
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
//                            PreferenceHelper.setNewsNotificationsEnabled(this, option.isActivated)
                        }
                    }
                }
            }

            StorageHelper.firstStart = true
            displayFirstPage(null)
        }
    }

    private fun setFragment(fragment: Fragment, newTitle: Int) {
        title = getString(newTitle)

        setFragment(fragment)
    }

    private fun setFragment(fragment: Fragment): Unit {
        supportFragmentManager.beginTransaction()
                .setAllowOptimization(true)
                .replace(R.id.container, fragment)
                .commitNow()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUserChanged(@Suppress("UNUSED_PARAMETER") event: UserChangedEvent) {
        drawer.refreshHeader(this)
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLogout(@Suppress("UNUSED_PARAMETER") event: LogoutEvent) {
        drawer.refreshHeader(this)
    }

    private fun displayFirstPage(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            if (StorageHelper.firstStart) {
                IntroductionHelper(this)
            } else {
                drawer.select(getItemToLoad())
            }
        }
    }

    private fun getItemToLoad() = DrawerItem.fromOrDefault(when (intent.action == Intent.ACTION_VIEW) {
        true -> intent.dataString.toLongOrNull()
        false -> intent.getLongExtra(EXTRA_DRAWER_ITEM, PreferenceHelper.getStartPage(this).id)
    })

    private fun onDrawerItemClick(item: DrawerItem): Boolean {
        when (item) {
            DrawerItem.NEWS -> {
                setFragment(NewsArticleFragment.newInstance(), R.string.section_news)

                return false
            }

            DrawerItem.CHAT -> {
//                setFragment(ConferencesFragment.newInstance(), R.string.fragment_conferences)

                return false
            }

            DrawerItem.BOOKMARKS -> {
//                setFragment(ReminderFragment.newInstance(), R.string.fragment_reminder)

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
        }
    }

    private fun onAccountItemClick(item: AccountItem): Boolean {
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
//                StorageHelper.user?.let {
//                    ProfileActivity.navigateTo(this, it.id, it.username, it.imageId)
//                }

                return false
            }

            AccountItem.UCP -> {
                UcpActivity.navigateTo(this)

                return false
            }
        }
    }
}
