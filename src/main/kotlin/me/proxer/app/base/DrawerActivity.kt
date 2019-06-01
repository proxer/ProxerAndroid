package me.proxer.app.base

import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import com.google.android.material.appbar.AppBarLayout
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import io.reactivex.android.schedulers.AndroidSchedulers
import kotterknife.bindView
import me.proxer.app.MainActivity
import me.proxer.app.R
import me.proxer.app.auth.LoginDialog
import me.proxer.app.auth.LogoutDialog
import me.proxer.app.notification.NotificationActivity
import me.proxer.app.profile.ProfileActivity
import me.proxer.app.ucp.UcpActivity
import me.proxer.app.ucp.settings.UcpSettingsActivity
import me.proxer.app.util.wrapper.MaterialDrawerWrapper
import me.proxer.app.util.wrapper.MaterialDrawerWrapper.AccountItem
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
@Suppress("UnnecessaryAbstractClass")
abstract class DrawerActivity : BaseActivity() {

    protected open val contentView
        get() = R.layout.activity_default

    protected var drawer by Delegates.notNull<MaterialDrawerWrapper>()
        private set

    protected open val isRootActivity = false
    protected open val isMainActivity = false

    protected open val toolbar: Toolbar by bindView(R.id.toolbar)
    protected open val appbar: AppBarLayout by bindView(R.id.appbar)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(contentView)
        setSupportActionBar(toolbar)

        drawer = MaterialDrawerWrapper(this, toolbar, savedInstanceState, isRootActivity, isMainActivity).also {
            it.itemClickSubject
                .autoDisposable(this.scope())
                .subscribe { item -> handleDrawerItemClick(item) }

            it.accountClickSubject
                .autoDisposable(this.scope())
                .subscribe { item -> handleAccountItemClick(item) }
        }

        storageHelper.isLoggedInObservable
            .observeOn(AndroidSchedulers.mainThread())
            .autoDisposable(this.scope())
            .subscribe { drawer.refreshHeader(this) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        drawer.saveInstanceState(outState)
    }

    override fun onBackPressed() {
        if (!drawer.onBackPressed()) {
            super.onBackPressed()
        }
    }

    protected open fun handleDrawerItemClick(item: MaterialDrawerWrapper.DrawerItem) {
        MainActivity.navigateToSection(this, item)
    }

    protected open fun handleAccountItemClick(item: AccountItem) = when (item) {
        AccountItem.GUEST, AccountItem.LOGIN -> LoginDialog.show(this)
        AccountItem.LOGOUT -> LogoutDialog.show(this)
        AccountItem.USER -> showProfilePage()
        AccountItem.NOTIFICATIONS -> NotificationActivity.navigateTo(this)
        AccountItem.UCP -> UcpActivity.navigateTo(this)
        AccountItem.PROFILE_SETTINGS -> UcpSettingsActivity.navigateTo(this)
    }

    private fun showProfilePage() = storageHelper.user?.let { (_, id, name, image) ->
        ProfileActivity.navigateTo(this, id, name, image, null)
    }
}
