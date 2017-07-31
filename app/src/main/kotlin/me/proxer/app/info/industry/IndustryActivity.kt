package me.proxer.app.info.industry

import android.app.Activity
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.app.ShareCompat
import android.view.Menu
import android.view.MenuItem
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import me.proxer.app.R
import me.proxer.app.base.ImageTabsActivity
import me.proxer.library.util.ProxerUrls
import org.jetbrains.anko.intentFor

/**
 * @author Ruben Gees
 */
class IndustryActivity : ImageTabsActivity() {

    companion object {
        private const val ID_EXTRA = "id"
        private const val NAME_EXTRA = "name"

        fun navigateTo(context: Activity, id: String, name: String? = null) {
            context.startActivity(context.intentFor<IndustryActivity>(
                    ID_EXTRA to id,
                    NAME_EXTRA to name
            ))
        }
    }

    val id: String
        get() = intent.getStringExtra(ID_EXTRA)

    var name: String?
        get() = intent.getStringExtra(NAME_EXTRA)
        set(value) {
            intent.putExtra(NAME_EXTRA, value)

            title = value
        }

    override val headerImageUrl by lazy { ProxerUrls.industryImage(id) }
    override val sectionsPagerAdapter by lazy { SectionsPagerAdapter(supportFragmentManager) }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        IconicsMenuInflaterUtil.inflate(menuInflater, this, R.menu.activity_share, menu, true)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_share -> {
                name?.let {
                    ShareCompat.IntentBuilder
                            .from(this)
                            .setText(getString(R.string.share_industry, it,
                                    "https://proxer.me/industry?id=$id"))
                            .setType("text/plain")
                            .setChooserTitle(getString(R.string.share_title))
                            .startChooser()
                }
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun setupToolbar() {
        super.setupToolbar()

        title = name
    }

    inner class SectionsPagerAdapter(fragmentManager: FragmentManager) : FragmentPagerAdapter(fragmentManager) {

        override fun getItem(position: Int) = when (position) {
            0 -> IndustryInfoFragment.newInstance()
            1 -> IndustryProjectFragment.newInstance()
            else -> throw RuntimeException("Unknown index passed")
        }

        override fun getCount() = 2

        override fun getPageTitle(position: Int): String = when (position) {
            0 -> getString(R.string.section_industry_info)
            1 -> getString(R.string.section_industry_projects)
            else -> throw RuntimeException("Unknown index passed")
        }
    }
}
