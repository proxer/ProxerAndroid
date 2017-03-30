package me.proxer.app.fragment.base

import android.os.Bundle
import android.support.v4.app.Fragment
import me.proxer.app.application.MainApplication
import me.proxer.app.util.extension.KotterKnife
import me.proxer.app.util.extension.androidUri
import me.proxer.app.util.extension.openHttpPage
import me.zhanghai.android.customtabshelper.CustomTabsHelperFragment
import okhttp3.HttpUrl

/**
 * @author Ruben Gees
 */
abstract class MainFragment : Fragment() {

    private lateinit var customTabsHelper: CustomTabsHelperFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        customTabsHelper = CustomTabsHelperFragment.attachTo(this)
    }

    override fun onDestroyView() {
        KotterKnife.reset(this)

        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()

        MainApplication.refWatcher.watch(this)
    }

    fun setLikelyUrl(url: HttpUrl) {
        customTabsHelper.mayLaunchUrl(url.androidUri(), Bundle(), emptyList())
    }

    fun showPage(url: HttpUrl) {
        customTabsHelper.openHttpPage(activity, url)
    }
}
