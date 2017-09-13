package me.proxer.app.ucp

import android.app.Activity
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import com.h6ah4i.android.tablayouthelper.TabLayoutHelper
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseActivity
import me.proxer.app.ucp.history.HistoryFragment
import me.proxer.app.ucp.overview.UcpOverviewFragment
import me.proxer.app.ucp.topten.UcpTopTenFragment
import org.jetbrains.anko.startActivity

class UcpActivity : BaseActivity() {

    companion object {
        fun navigateTo(context: Activity) = context.startActivity<UcpActivity>()
    }

    private var sectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)

    private val toolbar: Toolbar by bindView(R.id.toolbar)
    private val viewPager: ViewPager by bindView(R.id.viewPager)
    private val tabs: TabLayout by bindView(R.id.tabs)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_ucp)
        setSupportActionBar(toolbar)

        setupToolbar()

        viewPager.adapter = sectionsPagerAdapter

        TabLayoutHelper(tabs, viewPager).apply { isAutoAdjustTabModeEnabled = true }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()

                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.section_ucp)
    }

    inner class SectionsPagerAdapter(fragmentManager: FragmentManager) : FragmentPagerAdapter(fragmentManager) {

        override fun getItem(position: Int) = when (position) {
            0 -> UcpOverviewFragment.newInstance()
            1 -> UcpTopTenFragment.newInstance()
            2 -> HistoryFragment.newInstance()
            else -> throw IllegalArgumentException("Unknown index passed: $position")
        }

        override fun getCount() = 3

        override fun getPageTitle(position: Int): String = when (position) {
            0 -> getString(R.string.section_ucp_overview)
            1 -> getString(R.string.section_ucp_top_ten)
            2 -> getString(R.string.section_ucp_history)
            else -> throw IllegalArgumentException("Unknown index passed: $position")
        }
    }
}
