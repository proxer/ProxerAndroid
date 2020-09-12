package me.proxer.app.base

import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.rubengees.rxbus.RxBus
import kotterknife.KotterKnife
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.androidUri
import me.proxer.app.util.extension.fallbackHandleLink
import me.proxer.app.util.extension.safeInject
import me.zhanghai.android.customtabshelper.CustomTabsHelperFragment
import okhttp3.HttpUrl
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
@Suppress("UnnecessaryAbstractClass")
abstract class BaseDialog : DialogFragment(), CustomTabsAware {

    val dialogLifecycleOwner: LifecycleOwner = object : LifecycleOwner {

        override fun getLifecycle(): Lifecycle {
            return lifecycleRegistry ?: LifecycleRegistry(this).also {
                lifecycleRegistry = it
            }
        }
    }

    protected val bus by safeInject<RxBus>()
    protected val storageHelper by safeInject<StorageHelper>()
    protected val preferenceHelper by safeInject<PreferenceHelper>()

    protected var customTabsHelper by Delegates.notNull<CustomTabsHelperFragment>()
        private set

    private var lifecycleRegistry: LifecycleRegistry? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        customTabsHelper = CustomTabsHelperFragment.attachTo(this)
    }

    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        val result = super.onGetLayoutInflater(savedInstanceState)

        if (dialog != null) {
            lifecycleRegistry?.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        }

        onDialogCreated(savedInstanceState)

        return result
    }

    override fun onStart() {
        super.onStart()

        if (dialog != null) {
            lifecycleRegistry?.handleLifecycleEvent(Lifecycle.Event.ON_START)
        }
    }

    override fun onResume() {
        super.onResume()

        if (dialog != null) {
            lifecycleRegistry?.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
    }

    override fun onPause() {
        super.onPause()

        if (dialog != null) {
            lifecycleRegistry?.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        }
    }

    override fun onStop() {
        if (dialog != null) {
            lifecycleRegistry?.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }

        super.onStop()
    }

    override fun onDestroyView() {
        if (dialog != null) {
            lifecycleRegistry?.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }

        KotterKnife.reset(this)

        super.onDestroyView()
    }

    override fun setLikelyUrl(url: HttpUrl): Boolean {
        return customTabsHelper.mayLaunchUrl(url.androidUri(), bundleOf(), emptyList())
    }

    override fun showPage(url: HttpUrl, forceBrowser: Boolean, skipCheck: Boolean) {
        customTabsHelper.fallbackHandleLink(requireActivity(), url, forceBrowser, skipCheck)
    }

    open fun onDialogCreated(savedInstanceState: Bundle?) = Unit
}
