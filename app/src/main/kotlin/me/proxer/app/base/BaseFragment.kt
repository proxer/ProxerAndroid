package me.proxer.app.base

import android.arch.lifecycle.LifecycleFragment
import android.os.Bundle
import me.proxer.app.MainApplication.Companion.refWatcher
import me.proxer.app.util.extension.androidUri
import me.proxer.app.util.extension.openHttpPage
import me.zhanghai.android.customtabshelper.CustomTabsHelperFragment
import okhttp3.HttpUrl

/**
 * @author Ruben Gees
 */
abstract class BaseFragment : LifecycleFragment() {

    private val customTabsHelper by lazy { CustomTabsHelperFragment.attachTo(this) }

    override fun onDestroy() {
        super.onDestroy()

        refWatcher.watch(this)
    }

    fun setLikelyUrl(url: HttpUrl) {
        customTabsHelper.mayLaunchUrl(url.androidUri(), Bundle(), emptyList())
    }

    fun showPage(url: HttpUrl) {
        customTabsHelper.openHttpPage(activity, url)
    }
}
