package com.proxerme.app.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import com.proxerme.app.R
import com.proxerme.app.dialog.LoginDialog
import com.proxerme.app.dialog.LogoutDialog
import com.proxerme.app.fragment.SettingsFragment
import com.proxerme.app.fragment.chat.ConferencesFragment
import com.proxerme.app.fragment.media.MediaListFragment
import com.proxerme.app.fragment.news.NewsFragment
import com.proxerme.app.fragment.ucp.ReminderFragment
import com.proxerme.app.helper.IntroductionHelper
import com.proxerme.app.helper.MaterialDrawerHelper
import com.proxerme.app.helper.MaterialDrawerHelper.Companion.ACCOUNT_GUEST
import com.proxerme.app.helper.MaterialDrawerHelper.Companion.ACCOUNT_LOGIN
import com.proxerme.app.helper.MaterialDrawerHelper.Companion.ACCOUNT_LOGOUT
import com.proxerme.app.helper.MaterialDrawerHelper.Companion.ACCOUNT_UCP
import com.proxerme.app.helper.MaterialDrawerHelper.Companion.ACCOUNT_USER
import com.proxerme.app.helper.MaterialDrawerHelper.Companion.ITEM_ANIME
import com.proxerme.app.helper.MaterialDrawerHelper.Companion.ITEM_CHAT
import com.proxerme.app.helper.MaterialDrawerHelper.Companion.ITEM_DONATE
import com.proxerme.app.helper.MaterialDrawerHelper.Companion.ITEM_MANGA
import com.proxerme.app.helper.MaterialDrawerHelper.Companion.ITEM_NEWS
import com.proxerme.app.helper.MaterialDrawerHelper.Companion.ITEM_REMINDER
import com.proxerme.app.helper.MaterialDrawerHelper.Companion.ITEM_SETTINGS
import com.proxerme.app.helper.PreferenceHelper
import com.proxerme.app.helper.StorageHelper
import com.proxerme.app.interfaces.OnActivityListener
import com.proxerme.app.manager.UserManager
import com.proxerme.app.module.CustomTabsModule
import com.proxerme.app.util.bindView
import com.proxerme.library.info.ProxerUrlHolder
import com.proxerme.library.parameters.CategoryParameter
import com.rubengees.introduction.IntroductionActivity.OPTION_RESULT
import com.rubengees.introduction.IntroductionBuilder
import com.rubengees.introduction.entity.Option
import customtabs.CustomTabActivityHelper
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.intentFor

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class DashboardActivity : AppCompatActivity(), CustomTabsModule {

    companion object {
        private const val STATE_TITLE = "activity_dashboard_title"
        private const val EXTRA_DRAWER_ITEM = "extra_drawer_item"

        fun getSectionIntent(context: Context,
                             @MaterialDrawerHelper.Companion.DrawerItem itemId: Long): Intent {
            return context.intentFor<DashboardActivity>(EXTRA_DRAWER_ITEM to itemId)
        }
    }

    override val customTabActivityHelper: CustomTabActivityHelper = CustomTabActivityHelper()

    private lateinit var drawer: MaterialDrawerHelper
    private var onActivityListener: OnActivityListener? = null

    private var title: String? = null

    private val toolbar: Toolbar by bindView(R.id.toolbar)
    private val appbar: AppBarLayout by bindView(R.id.appbar)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_default)
        setSupportActionBar(toolbar)

        drawer = MaterialDrawerHelper(this, toolbar, savedInstanceState,
                { id -> onDrawerItemClick(id) }, { id -> onAccountItemClick(id) })

        displayFirstPage(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()

        drawer.refreshHeader(this)
    }

    override fun onStart() {
        super.onStart()

        try {
            customTabActivityHelper.bindCustomTabsService(this)
        } catch(ignored: Exception) {
            // Workaround for crash if chrome is not installed
        }

        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        customTabActivityHelper.unbindCustomTabsService(this)

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
            val lastFragment = supportFragmentManager.findFragmentById(R.id.container)

            when (lastFragment) {
                is OnActivityListener -> onActivityListener = lastFragment
                else -> onActivityListener = null
            }

            title = savedInstanceState.getString(STATE_TITLE)

            setTitle(title)
        }
    }

    override fun onBackPressed() {
        if (drawer.isDrawerOpen()) {
            drawer.onBackPressed()
        } else {
            if (!(onActivityListener?.onBackPressed() ?: false)) {
                if (!drawer.onBackPressed()) {
                    super.onBackPressed()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == IntroductionBuilder.INTRODUCTION_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                for (option in data.getParcelableArrayListExtra<Option>(OPTION_RESULT)) {
                    when (option.position) {
                        1 -> {
                            PreferenceHelper.setNewsNotificationsEnabled(this, option.isActivated)
                        }
                    }
                }
            }

            StorageHelper.firstStart = true
            drawer.select(ITEM_NEWS)
        }
    }

    private fun setFragment(fragment: Fragment, title: String) {
        this.title = title

        setTitle(title)
        setFragment(fragment)
    }

    private fun setFragment(fragment: Fragment): Unit {
        when (fragment) {
            is OnActivityListener -> onActivityListener = fragment
            else -> onActivityListener = null
        }

        appbar.setExpanded(true, true)
        supportFragmentManager.beginTransaction().replace(R.id.container, fragment)
                .commitNow()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLoginStateChanged(@Suppress("UNUSED_PARAMETER") newState: UserManager.LoginState) {
        drawer.refreshHeader(this)
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOngoingStateChanged(@Suppress("UNUSED_PARAMETER") newState: UserManager.OngoingState) {
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

    @MaterialDrawerHelper.Companion.DrawerItem
    private fun getItemToLoad(): Long {
        return intent.getLongExtra(EXTRA_DRAWER_ITEM, MaterialDrawerHelper.ITEM_NEWS)
    }

    private fun onDrawerItemClick(id: Long): Boolean {
        when (id) {
            ITEM_NEWS -> {
                setFragment(NewsFragment.newInstance(), getString(R.string.fragment_news))

                return false
            }

            ITEM_CHAT -> {
                setFragment(ConferencesFragment.newInstance(),
                        getString(R.string.fragment_conferences))

                return false
            }

            ITEM_REMINDER -> {
                setFragment(ReminderFragment.newInstance(), getString(R.string.fragment_reminder))

                return false
            }

            ITEM_ANIME -> {
                setFragment(MediaListFragment.newInstance(CategoryParameter.ANIME),
                        getString(R.string.fragment_media_list_anime_title))

                return false
            }

            ITEM_MANGA -> {
                setFragment(MediaListFragment.newInstance(CategoryParameter.MANGA),
                        getString(R.string.fragment_media_list_manga_title))

                return false
            }

            ITEM_DONATE -> {
                showPage(ProxerUrlHolder.getDonateUrl("default"))

                return true
            }

            ITEM_SETTINGS -> {
                setFragment(SettingsFragment.newInstance(), getString(R.string.fragment_settings))

                return false
            }

            else -> return true
        }
    }

    private fun onAccountItemClick(id: Long): Boolean {
        when (id) {
            ACCOUNT_GUEST -> {
                LoginDialog.show(this)

                return false
            }

            ACCOUNT_LOGIN -> {
                LoginDialog.show(this)

                return false
            }

            ACCOUNT_LOGOUT -> {
                LogoutDialog.show(this)

                return false
            }

            ACCOUNT_USER -> {
                UserManager.user?.let {
                    UserActivity.navigateTo(this, it.id, it.username, it.imageId)
                }

                return false
            }

            ACCOUNT_UCP -> {
                UcpActivity.navigateTo(this)

                return false
            }

            else -> return false
        }
    }
}