package me.proxer.app.base

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.os.bundleOf
import com.google.android.material.snackbar.Snackbar
import com.rubengees.rxbus.RxBus
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.compat.TaskDescriptionCompat
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.androidUri
import me.proxer.app.util.extension.fallbackHandleLink
import me.proxer.app.util.extension.recursiveChildren
import me.proxer.app.util.extension.safeInject
import me.zhanghai.android.customtabshelper.CustomTabsHelperFragment
import okhttp3.HttpUrl
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
@Suppress("UnnecessaryAbstractClass")
abstract class BaseActivity : AppCompatActivity(), CustomTabsAware {

    private companion object {
        private const val STATE = "activity_state"
    }

    protected open val theme
        @StyleRes get() = preferenceHelper.themeContainer.theme.main

    protected open val root: ViewGroup by bindView(R.id.root)

    protected val bus by safeInject<RxBus>()
    protected val storageHelper by safeInject<StorageHelper>()
    protected val preferenceHelper by safeInject<PreferenceHelper>()

    private var customTabsHelper by Delegates.notNull<CustomTabsHelperFragment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        // This needs to be called before super.onCreate(), because otherwise Fragments might not see the
        // restored state in time.
        savedInstanceState?.getBundle(STATE)?.let { state ->
            intent.putExtras(state)
        }

        getTheme().applyStyle(theme, true)
        TaskDescriptionCompat.setTaskDescription(this, preferenceHelper.themeContainer.theme.primaryColor(this))

        super.onCreate(savedInstanceState)

        customTabsHelper = CustomTabsHelperFragment.attachTo(this)

        preferenceHelper.themeObservable
            .autoDisposable(this.scope())
            .subscribe { ActivityCompat.recreate(this) }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        intent.extras?.let { state ->
            outState.putBundle(STATE, state)
        }
    }

    override fun onBackPressed() {
        // Workaround for memory leak on Android 10: https://twitter.com/Piwai/status/1169274624749658112
        if (isTaskRoot && supportFragmentManager.backStackEntryCount == 0) {
            finishAfterTransition()
        } else {
            super.onBackPressed()
        }
    }

    override fun setLikelyUrl(url: HttpUrl): Boolean {
        return customTabsHelper.mayLaunchUrl(url.androidUri(), bundleOf(), emptyList())
    }

    override fun showPage(url: HttpUrl, forceBrowser: Boolean, skipCheck: Boolean) {
        customTabsHelper.fallbackHandleLink(this, url, forceBrowser, skipCheck)
    }

    fun snackbar(
        message: CharSequence,
        duration: Int = Snackbar.LENGTH_LONG,
        actionMessage: Int = ErrorUtils.ErrorAction.ACTION_MESSAGE_DEFAULT,
        actionCallback: View.OnClickListener? = null,
        maxLines: Int = -1
    ) {
        Snackbar.make(root, message, duration).apply {
            when (actionMessage) {
                ErrorUtils.ErrorAction.ACTION_MESSAGE_DEFAULT -> {
                    val multilineActionMessage = getString(R.string.error_action_retry).replace(" ", "\n")

                    setAction(multilineActionMessage, actionCallback)
                }
                ErrorUtils.ErrorAction.ACTION_MESSAGE_HIDE -> setAction(null, null)
                else -> setAction(actionMessage, actionCallback)
            }

            if (maxLines >= 0) {
                (view as ViewGroup).recursiveChildren
                    .filterIsInstance(TextView::class.java)
                    .forEach { it.maxLines = maxLines }
            }

            show()
        }
    }
}
