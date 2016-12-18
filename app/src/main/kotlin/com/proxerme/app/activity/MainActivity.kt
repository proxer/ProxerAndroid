package com.proxerme.app.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.proxerme.app.util.androidUri
import com.proxerme.app.util.openHttpPage
import me.zhanghai.android.customtabshelper.CustomTabsHelperFragment
import okhttp3.HttpUrl

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
abstract class MainActivity : AppCompatActivity() {

    private lateinit var customTabsHelper: CustomTabsHelperFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        customTabsHelper = CustomTabsHelperFragment.attachTo(this)
    }

    fun setLikelyUrl(url: HttpUrl) {
        customTabsHelper.mayLaunchUrl(url.androidUri(), Bundle(), emptyList())
    }

    fun showPage(url: HttpUrl) {
        customTabsHelper.openHttpPage(this, url)
    }
}