package com.proxerme.app.fragment.framework

import android.os.Bundle
import android.support.v4.app.Fragment
import com.proxerme.app.application.MainApplication
import com.proxerme.app.manager.SectionManager
import com.proxerme.app.util.extension.androidUri
import com.proxerme.app.util.extension.openHttpPage
import me.zhanghai.android.customtabshelper.CustomTabsHelperFragment
import okhttp3.HttpUrl

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
abstract class MainFragment : Fragment() {

    abstract val section: SectionManager.Section

    private lateinit var customTabsHelper: CustomTabsHelperFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        customTabsHelper = CustomTabsHelperFragment.attachTo(this)
    }

    override fun onResume() {
        super.onResume()

        SectionManager.notifySectionResumed(section)
    }

    override fun onPause() {
        super.onPause()

        SectionManager.notifySectionPaused(section)
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
