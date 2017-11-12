package me.proxer.app

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.view.ViewCompat
import android.support.v7.widget.Toolbar
import android.view.ViewGroup
import com.rubengees.introduction.IntroductionActivity.OPTION_RESULT
import com.rubengees.introduction.IntroductionBuilder
import com.rubengees.introduction.Option
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import kotterknife.bindView
import me.proxer.app.MainApplication.Companion.bus
import me.proxer.app.auth.LoginDialog
import me.proxer.app.auth.LoginEvent
import me.proxer.app.auth.LogoutDialog
import me.proxer.app.auth.LogoutEvent
import me.proxer.app.base.BackPressAware
import me.proxer.app.base.BaseActivity
import me.proxer.app.bookmark.BookmarkFragment
import me.proxer.app.chat.conference.ConferenceFragment
import me.proxer.app.manga.local.LocalMangaFragment
import me.proxer.app.media.list.MediaListFragment
import me.proxer.app.news.NewsFragment
import me.proxer.app.notification.NotificationActivity
import me.proxer.app.notification.NotificationJob
import me.proxer.app.profile.ProfileActivity
import me.proxer.app.settings.AboutFragment
import me.proxer.app.settings.SettingsFragment
import me.proxer.app.ucp.UcpActivity
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.autoDispose
import me.proxer.app.util.extension.shortcutManager
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.app.util.wrapper.IntroductionWrapper
import me.proxer.app.util.wrapper.MaterialDrawerWrapper
import me.proxer.app.util.wrapper.MaterialDrawerWrapper.AccountItem
import me.proxer.app.util.wrapper.MaterialDrawerWrapper.DrawerItem
import me.proxer.library.enums.Category
import me.proxer.library.enums.Device
import me.proxer.library.util.ProxerUrls
import org.jetbrains.anko.intentFor
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class MainActivity : BaseActivity() {

    companion object {
        private const val TITLE_STATE = "title"
        private const val SECTION_EXTRA = "section"

        private const val SHORTCUT_NEWS = "news"
        private const val SHORTCUT_CHAT = "chat"
        private const val SHORTCUT_BOOKMARKS = "bookmarks"

        fun getSectionIntent(context: Context, section: DrawerItem) = context
                .intentFor<MainActivity>(SECTION_EXTRA to section.id)
    }

    private var drawer by Delegates.notNull<MaterialDrawerWrapper>()

    private val root: ViewGroup by bindView(R.id.root)
    private val toolbar: Toolbar by bindView(R.id.toolbar)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_default)
        setSupportActionBar(toolbar)
        supportPostponeEnterTransition()

        drawer = MaterialDrawerWrapper(this, toolbar, savedInstanceState).also {
            it.itemClickSubject
                    .autoDispose(this)
                    .subscribe { handleDrawerItemClick(it) }

            it.accountClickSubject
                    .autoDispose(this)
                    .subscribe { handleAccountItemClick(it) }
        }

        Observable.merge(bus.register(LoginEvent::class.java), bus.register(LogoutEvent::class.java))
                .observeOn(AndroidSchedulers.mainThread())
                .autoDispose(this)
                .subscribe { drawer.refreshHeader() }

        displayFirstPage(savedInstanceState)

        root.postDelayed({
            supportStartPostponedEnterTransition()
        }, 50)
    }

    override fun onResume() {
        super.onResume()

        drawer.refreshHeader()
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
            if (supportFragmentManager.fragments.none { it is BackPressAware && it.onBackPressed() }) {
                val startPage = PreferenceHelper.getStartPage(this)

                if (startPage != drawer.currentItem) {
                    drawer.select(startPage)
                } else {
                    super.onBackPressed()
                }
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

                            Completable.fromAction { NotificationJob.scheduleIfPossible(this) }
                                    .autoDispose(this)
                                    .subscribeAndLogErrors()
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

    private fun setFragment(fragment: Fragment) = supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commitNow()

    private fun displayFirstPage(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            if (StorageHelper.isFirstStart) {
                IntroductionWrapper.introduce(this)
            } else {
                drawer.select(getItemToLoad())
            }
        }
    }

    private fun getItemToLoad() = DrawerItem.fromOrDefault(when (intent.action == Intent.ACTION_VIEW) {
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

    private fun handleDrawerItemClick(item: DrawerItem) = when (item) {
        DrawerItem.NEWS -> setFragment(NewsFragment.newInstance(), R.string.section_news)
        DrawerItem.CHAT -> setFragment(ConferenceFragment.newInstance(), R.string.section_chat)
        DrawerItem.BOOKMARKS -> setFragment(BookmarkFragment.newInstance(), R.string.section_bookmarks)
        DrawerItem.ANIME -> setFragment(MediaListFragment.newInstance(Category.ANIME), R.string.section_anime)
        DrawerItem.MANGA -> setFragment(MediaListFragment.newInstance(Category.MANGA), R.string.section_manga)
        DrawerItem.LOCAL_MANGA -> setFragment(LocalMangaFragment.newInstance(), R.string.section_local_manga)
        DrawerItem.INFO -> setFragment(AboutFragment.newInstance(), R.string.section_info)
        DrawerItem.DONATE -> showPage(ProxerUrls.donateWeb(Device.DEFAULT))
        DrawerItem.SETTINGS -> setFragment(SettingsFragment.newInstance(), R.string.section_settings)
    }

    private fun handleAccountItemClick(item: AccountItem) = when (item) {
        AccountItem.GUEST, AccountItem.LOGIN -> LoginDialog.show(this)
        AccountItem.LOGOUT -> LogoutDialog.show(this)
        AccountItem.USER -> showProfilePage()
        AccountItem.NOTIFICATIONS -> NotificationActivity.navigateTo(this)
        AccountItem.UCP -> UcpActivity.navigateTo(this)
    }

    private fun showProfilePage() = StorageHelper.user?.let {
        drawer.profileImageView.let { view ->
            ViewCompat.setTransitionName(view, "profile_image")

            ProfileActivity.navigateTo(this, it.id, it.name, it.image,
                    if (view.drawable != null) view else null)
        }
    }
}
