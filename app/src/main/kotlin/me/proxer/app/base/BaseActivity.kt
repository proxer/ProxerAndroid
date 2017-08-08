package me.proxer.app.base

import android.arch.lifecycle.LifecycleRegistry
import android.arch.lifecycle.LifecycleRegistryOwner
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import me.proxer.app.util.extension.androidUri
import me.proxer.app.util.extension.openHttpPage
import me.zhanghai.android.customtabshelper.CustomTabsHelperFragment
import okhttp3.HttpUrl
import org.jetbrains.anko.bundleOf

/**
 * @author Ruben Gees
 */
abstract class BaseActivity : AppCompatActivity(), LifecycleRegistryOwner {

    private lateinit var lifecycleRegistry: LifecycleRegistry
    private lateinit var customTabsHelper: CustomTabsHelperFragment

    fun setLikelyUrl(url: HttpUrl) = customTabsHelper.mayLaunchUrl(url.androidUri(), bundleOf(), emptyList())
    fun showPage(url: HttpUrl) = customTabsHelper.openHttpPage(this, url)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleRegistry = LifecycleRegistry(this)
        customTabsHelper = CustomTabsHelperFragment.attachTo(this)
    }

    override fun getLifecycle() = lifecycleRegistry
}
