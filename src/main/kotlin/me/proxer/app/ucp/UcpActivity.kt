package me.proxer.app.ucp

import android.app.Activity
import android.os.Bundle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.DrawerActivity
import me.proxer.app.ucp.history.UcpHistoryFragment
import me.proxer.app.ucp.media.UcpMediaListFragment
import me.proxer.app.ucp.overview.UcpOverviewFragment
import me.proxer.app.ucp.topten.UcpTopTenFragment
import me.proxer.app.util.extension.startActivity
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.enums.Category

/**
 * @author Ruben Gees
 */
class UcpActivity : DrawerActivity() {

    companion object {
        fun navigateTo(context: Activity) = context.startActivity<UcpActivity>()
    }

    override val contentView: Int
        get() = R.layout.activity_ucp

    private val sectionsPagerAdapter by unsafeLazy { SectionsPagerAdapter() }
    private val sectionsTabCallback by unsafeLazy { SectionsTabCallback() }

    private val viewPager: ViewPager2 by bindView(R.id.viewPager)
    private val tabs: TabLayout by bindView(R.id.tabs)

    private var mediator: TabLayoutMediator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupToolbar()

        viewPager.adapter = sectionsPagerAdapter

        mediator = TabLayoutMediator(tabs, viewPager, sectionsTabCallback).also { it.attach() }
    }

    override fun onDestroy() {
        mediator?.detach()
        mediator = null
        viewPager.adapter = null

        super.onDestroy()
    }

    private fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.section_ucp)
    }

    private inner class SectionsPagerAdapter : FragmentStateAdapter(supportFragmentManager, lifecycle) {

        override fun getItemCount() = 5

        override fun createFragment(position: Int) = when (position) {
            0 -> UcpOverviewFragment.newInstance()
            1 -> UcpTopTenFragment.newInstance()
            2 -> UcpMediaListFragment.newInstance(Category.ANIME)
            3 -> UcpMediaListFragment.newInstance(Category.MANGA)
            4 -> UcpHistoryFragment.newInstance()
            else -> error("Unknown index passed: $position")
        }
    }

    private inner class SectionsTabCallback : TabLayoutMediator.OnConfigureTabCallback {

        override fun onConfigureTab(tab: TabLayout.Tab, position: Int) {
            tab.text = when (position) {
                0 -> getString(R.string.section_ucp_overview)
                1 -> getString(R.string.section_ucp_top_ten)
                2 -> getString(R.string.section_user_media_list_anime)
                3 -> getString(R.string.section_user_media_list_manga)
                4 -> getString(R.string.section_ucp_history)
                else -> error("Unknown index passed: $position")
            }
        }
    }
}
