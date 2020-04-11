package me.proxer.app.base

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.WindowManager
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.appbar.AppBarLayout
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import kotterknife.bindView
import me.proxer.app.MainActivity
import me.proxer.app.R
import me.proxer.app.auth.LoginDialog
import me.proxer.app.auth.LogoutDialog
import me.proxer.app.notification.NotificationActivity
import me.proxer.app.profile.ProfileActivity
import me.proxer.app.profile.settings.ProfileSettingsActivity
import me.proxer.app.util.wrapper.MaterialDrawerWrapper
import me.proxer.app.util.wrapper.MaterialDrawerWrapper.ProfileItem
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

    private lateinit var actionBarDrawerToggle: ActionBarDrawerToggle

    open val drawerLayout: DrawerLayout by bindView(R.id.root)
    open val toolbar: Toolbar by bindView(R.id.toolbar)
    open val appbar: AppBarLayout by bindView(R.id.appbar)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(contentView)
        setSupportActionBar(toolbar)

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.TRANSPARENT

        actionBarDrawerToggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            com.mikepenz.materialdrawer.R.string.material_drawer_open,
            com.mikepenz.materialdrawer.R.string.material_drawer_close
        ).apply {
            setToolbarNavigationClickListener { onBackPressed() }
            drawerLayout.addDrawerListener(this)
            syncState()
        }

        if (!isRootActivity) {
            actionBarDrawerToggle.isDrawerIndicatorEnabled = false

            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        drawer = MaterialDrawerWrapper(this, toolbar, savedInstanceState, isMainActivity).also {
            it.itemClickSubject
                .autoDisposable(this.scope())
                .subscribe { item -> handleDrawerItemClick(item) }

            it.profileClickSubject
                .autoDisposable(this.scope())
                .subscribe { item -> handleAccountItemClick(item) }
        }

        storageHelper.isLoggedInObservable
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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        actionBarDrawerToggle.onConfigurationChanged(newConfig)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        actionBarDrawerToggle.syncState()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when {
            actionBarDrawerToggle.onOptionsItemSelected(item) -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    protected open fun handleDrawerItemClick(item: MaterialDrawerWrapper.DrawerItem) {
        MainActivity.navigateToSection(this, item)
    }

    protected open fun handleAccountItemClick(item: ProfileItem) = when (item) {
        ProfileItem.GUEST, ProfileItem.LOGIN -> LoginDialog.show(this)
        ProfileItem.LOGOUT -> LogoutDialog.show(this)
        ProfileItem.USER -> showProfilePage()
        ProfileItem.NOTIFICATIONS -> NotificationActivity.navigateTo(this)
        ProfileItem.PROFILE_SETTINGS -> ProfileSettingsActivity.navigateTo(this)
    }

    private fun showProfilePage() = storageHelper.user?.let { (_, id, name, image) ->
        ProfileActivity.navigateTo(this, id, name, image, null)
    }
}
