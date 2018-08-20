package me.proxer.app.base

import android.os.Bundle
import androidx.fragment.app.Fragment
import kotterknife.KotterKnife
import me.proxer.app.MainApplication.Companion.refWatcher
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
abstract class BaseFragment : Fragment() {

    protected open val hostingActivity: BaseActivity
        get() = requireActivity() as BaseActivity

    private var customTabsHelper by Delegates.notNull<CustomTabsHelperFragment>()

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

        refWatcher.watch(this)
    }

    fun setLikelyUrl(url: HttpUrl): Boolean {
        return customTabsHelper.mayLaunchUrl(url.androidUri(), bundleOf(), emptyList())
    }

    fun showPage(url: HttpUrl, forceBrowser: Boolean = false) {
        customTabsHelper.openHttpPage(requireActivity(), url, forceBrowser)
    }

    protected fun requireArguments() = arguments ?: throw IllegalStateException("arguments are null")
}
