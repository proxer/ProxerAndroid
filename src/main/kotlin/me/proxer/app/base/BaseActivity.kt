package me.proxer.app.base

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.extension.androidUri
import me.proxer.app.util.extension.openHttpPage
import me.zhanghai.android.customtabshelper.CustomTabsHelperFragment
import okhttp3.HttpUrl
import org.jetbrains.anko.bundleOf
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
@Suppress("UnnecessaryAbstractClass")
abstract class BaseActivity : AppCompatActivity() {

    private companion object {
        private const val STATE = "activity_state"
    }

    private var currentNightMode by Delegates.notNull<Int>()
    private var customTabsHelper by Delegates.notNull<CustomTabsHelperFragment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        // This needs to be called before super.onCreate(), because otherwise Fragments might not see the
        // restored state in time.
        savedInstanceState?.getBundle(STATE)?.let { state ->
            intent.putExtras(state)
        }

        super.onCreate(savedInstanceState)

        currentNightMode = PreferenceHelper.getNightMode(this)
        customTabsHelper = CustomTabsHelperFragment.attachTo(this)
    }

    override fun onResume() {
        val newNightMode = PreferenceHelper.getNightMode(this)

        if (currentNightMode != newNightMode) {
            currentNightMode = newNightMode

            recreate()
        }

        super.onResume()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        intent.extras?.let { state ->
            outState.putBundle(STATE, state)
        }
    }

    /* Workaround for a bug in the Android menu management */
    @Suppress("OverridingDeprecatedMember")
    override fun supportInvalidateOptionsMenu() {
        this.runOnUiThread {
            super.invalidateOptionsMenu()
        }
    }

    fun setLikelyUrl(url: HttpUrl): Boolean {
        return customTabsHelper.mayLaunchUrl(url.androidUri(), bundleOf(), emptyList())
    }

    fun showPage(url: HttpUrl, forceBrowser: Boolean = false) {
        customTabsHelper.openHttpPage(this, url, forceBrowser)
    }
}
