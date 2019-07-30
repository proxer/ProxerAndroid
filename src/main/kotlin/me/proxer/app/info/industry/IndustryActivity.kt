package me.proxer.app.info.industry

import android.app.Activity
import android.view.Menu
import android.view.MenuItem
import androidx.core.app.ShareCompat
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import me.proxer.app.R
import me.proxer.app.base.ImageTabsActivity
import me.proxer.app.util.extension.getSafeStringExtra
import me.proxer.app.util.extension.startActivity
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.util.ProxerUrls
import okhttp3.HttpUrl

/**
 * @author Ruben Gees
 */
class IndustryActivity : ImageTabsActivity() {

    companion object {
        private const val ID_EXTRA = "id"
        private const val NAME_EXTRA = "name"

        fun navigateTo(context: Activity, id: String, name: String? = null) {
            context.startActivity<IndustryActivity>(
                ID_EXTRA to id,
                NAME_EXTRA to name
            )
        }
    }

    val id: String
        get() = intent.getSafeStringExtra(ID_EXTRA)

    var name: String?
        get() = intent.getStringExtra(NAME_EXTRA)
        set(value) {
            intent.putExtra(NAME_EXTRA, value)

            title = value
        }

    override val headerImageUrl: HttpUrl by unsafeLazy { ProxerUrls.industryImage(id) }
    override val sectionsPagerAdapter: FragmentStateAdapter by unsafeLazy { SectionsPagerAdapter() }
    override val sectionsTabCallback: TabLayoutMediator.OnConfigureTabCallback by unsafeLazy { SectionsTabCallback() }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        IconicsMenuInflaterUtil.inflate(menuInflater, this, R.menu.activity_share, menu, true)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_share -> name?.let {
                ShareCompat.IntentBuilder
                    .from(this)
                    .setText(getString(R.string.share_industry, it, ProxerUrls.industryWeb(id)))
                    .setType("text/plain")
                    .setChooserTitle(getString(R.string.share_title))
                    .startChooser()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun setupToolbar() {
        super.setupToolbar()

        title = name
    }

    private inner class SectionsPagerAdapter : FragmentStateAdapter(supportFragmentManager, lifecycle) {

        override fun getItemCount() = 2

        override fun createFragment(position: Int) = when (position) {
            0 -> IndustryInfoFragment.newInstance()
            1 -> IndustryProjectFragment.newInstance()
            else -> error("Unknown index passed: $position")
        }
    }

    private inner class SectionsTabCallback : TabLayoutMediator.OnConfigureTabCallback {

        override fun onConfigureTab(tab: TabLayout.Tab, position: Int) {
            tab.text = when (position) {
                0 -> getString(R.string.section_industry_info)
                1 -> getString(R.string.section_industry_projects)
                else -> error("Unknown index passed: $position")
            }
        }
    }
}
