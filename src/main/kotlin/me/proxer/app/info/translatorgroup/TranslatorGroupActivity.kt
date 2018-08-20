package me.proxer.app.info.translatorgroup

import android.app.Activity
import android.view.Menu
import android.view.MenuItem
import androidx.core.app.ShareCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import me.proxer.app.R
import me.proxer.app.base.ImageTabsActivity
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.util.ProxerUrls
import okhttp3.HttpUrl
import org.jetbrains.anko.intentFor

/**
 * @author Ruben Gees
 */
class TranslatorGroupActivity : ImageTabsActivity() {

    companion object {
        private const val ID_EXTRA = "id"
        private const val NAME_EXTRA = "name"

        fun navigateTo(context: Activity, id: String, name: String? = null) {
            context.startActivity(context.intentFor<TranslatorGroupActivity>(
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

    override val headerImageUrl: HttpUrl by unsafeLazy { ProxerUrls.translatorGroupImage(id) }
    override val sectionsPagerAdapter by unsafeLazy { SectionsPagerAdapter(supportFragmentManager) }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        IconicsMenuInflaterUtil.inflate(menuInflater, this, R.menu.activity_share, menu, true)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_share -> name?.let {
                ShareCompat.IntentBuilder
                    .from(this)
                    .setText(getString(R.string.share_translator_group, it,
                        "https://proxer.me/translatorgroups?id=$id"))
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

    inner class SectionsPagerAdapter(fragmentManager: FragmentManager) : FragmentPagerAdapter(fragmentManager) {

        override fun getItem(position: Int) = when (position) {
            0 -> TranslatorGroupInfoFragment.newInstance()
            1 -> TranslatorGroupProjectFragment.newInstance()
            else -> throw IllegalArgumentException("Unknown index passed: $position")
        }

        override fun getCount() = 2

        override fun getPageTitle(position: Int): String = when (position) {
            0 -> getString(R.string.section_translator_group_info)
            1 -> getString(R.string.section_translator_group_projects)
            else -> throw IllegalArgumentException("Unknown index passed: $position")
        }
    }
}
