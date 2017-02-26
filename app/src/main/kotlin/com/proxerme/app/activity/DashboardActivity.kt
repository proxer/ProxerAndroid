package com.proxerme.app.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.v4.app.Fragment
import android.support.v7.widget.Toolbar
import com.proxerme.app.R
import com.proxerme.app.dialog.LoginDialog
import com.proxerme.app.dialog.LogoutDialog
import com.proxerme.app.event.LoginEvent
import com.proxerme.app.event.LogoutEvent
import com.proxerme.app.fragment.AboutFragment
import com.proxerme.app.fragment.SettingsFragment
import com.proxerme.app.fragment.chat.ConferencesFragment
import com.proxerme.app.fragment.media.MediaListFragment
import com.proxerme.app.fragment.news.NewsFragment
import com.proxerme.app.fragment.ucp.ReminderFragment
import com.proxerme.app.helper.IntroductionHelper
import com.proxerme.app.helper.MaterialDrawerHelper
import com.proxerme.app.helper.MaterialDrawerHelper.AccountItem
import com.proxerme.app.helper.MaterialDrawerHelper.DrawerItem
import com.proxerme.app.helper.PreferenceHelper
import com.proxerme.app.helper.StorageHelper
import com.proxerme.app.util.bindView
import com.proxerme.library.info.ProxerUrlHolder
import com.proxerme.library.parameters.CategoryParameter
import com.rubengees.introduction.IntroductionActivity.OPTION_RESULT
import com.rubengees.introduction.IntroductionBuilder
import com.rubengees.introduction.entity.Option
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.intentFor

/**
 * TODO: Describe class
 *
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

    private var title: String? = null

    private val toolbar: Toolbar by bindView(R.id.toolbar)
    private val appbar: AppBarLayout by bindView(R.id.appbar)

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

        outState.putString(STATE_TITLE, title)
        drawer.saveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)

        if (savedInstanceState != null) {
            title = savedInstanceState.getString(STATE_TITLE)

            setTitle(title)
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
                            PreferenceHelper.setNewsNotificationsEnabled(this, option.isActivated)
                        }
                    }
                }
            }

            StorageHelper.firstStart = true
            drawer.select(DrawerItem.NEWS)
        }
    }

    private fun setFragment(fragment: Fragment, title: Int) {
        this.title = getString(title)

        setTitle(title)
        setFragment(fragment)
    }

    private fun setFragment(fragment: Fragment): Unit {
        appbar.setExpanded(true, true)
        supportFragmentManager.beginTransaction()
                .setAllowOptimization(true)
                .replace(R.id.container, fragment)
                .commitNow()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLogin(@Suppress("UNUSED_PARAMETER") event: LoginEvent) {
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
                setFragment(NewsFragment.newInstance(), R.string.fragment_news)

                return false
            }

            DrawerItem.CHAT -> {
                setFragment(ConferencesFragment.newInstance(), R.string.fragment_conferences)

                return false
            }

            DrawerItem.REMINDER -> {
                setFragment(ReminderFragment.newInstance(), R.string.fragment_reminder)

                return false
            }

            DrawerItem.ANIME -> {
                setFragment(MediaListFragment.newInstance(CategoryParameter.ANIME),
                        R.string.fragment_media_list_anime_title)

                return false
            }

            DrawerItem.MANGA -> {
                setFragment(MediaListFragment.newInstance(CategoryParameter.MANGA),
                        R.string.fragment_media_list_manga_title)

                return false
            }

            DrawerItem.INFO -> {
                setFragment(AboutFragment.newInstance(), R.string.fragment_about_title)

                return false
            }

            DrawerItem.DONATE -> {
                showPage(ProxerUrlHolder.getDonateUrl("default"))

                return true
            }

            DrawerItem.SETTINGS -> {
                setFragment(SettingsFragment.newInstance(), R.string.fragment_settings)

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
                StorageHelper.user?.let {
                    ProfileActivity.navigateTo(this, it.id, it.username, it.imageId)
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
