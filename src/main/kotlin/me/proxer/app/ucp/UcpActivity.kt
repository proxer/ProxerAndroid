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
import me.proxer.app.ucp.overview.UcpOverviewFragment
import me.proxer.app.util.extension.startActivity
import me.proxer.app.util.extension.unsafeLazy

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

        viewPager.offscreenPageLimit = 2
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

        override fun getItemCount() = 1

        override fun createFragment(position: Int) = when (position) {
            0 -> UcpOverviewFragment.newInstance()
            else -> error("Unknown index passed: $position")
        }
    }

    private inner class SectionsTabCallback : TabLayoutMediator.TabConfigurationStrategy {

        override fun onConfigureTab(tab: TabLayout.Tab, position: Int) {
            tab.text = when (position) {
                0 -> getString(R.string.section_ucp_overview)
                else -> error("Unknown index passed: $position")
            }
        }
    }
}
