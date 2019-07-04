package me.proxer.app.comment

import android.app.Activity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Observer
import androidx.viewpager.widget.ViewPager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.h6ah4i.android.tablayouthelper.TabLayoutHelper
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.DrawerActivity
import me.proxer.app.util.extension.multilineSnackbar
import me.proxer.app.util.extension.startActivity
import me.proxer.app.util.extension.toast
import me.proxer.app.util.extension.unsafeLazy
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

/**
 * @author Ruben Gees
 */
class CommentActivity : DrawerActivity() {

    companion object {
        private const val ID_ARGUMENT = "id"
        private const val ENTRY_ID_ARGUMENT = "entry_id"
        private const val NAME_ARGUMENT = "name_id"

        fun navigateTo(
            context: Activity,
            id: String? = null,
            entryId: String? = null,
            name: String? = null
        ) = context.startActivity<CommentActivity>(
            ID_ARGUMENT to id,
            ENTRY_ID_ARGUMENT to entryId,
            NAME_ARGUMENT to name
        )
    }

    override val contentView: Int
        get() = R.layout.activity_comment

    val id: String?
        get() = intent.getStringExtra(ID_ARGUMENT)

    val entryId: String?
        get() = intent.getStringExtra(ENTRY_ID_ARGUMENT)

    val name: String?
        get() = intent.getStringExtra(NAME_ARGUMENT)

    private val viewModel by viewModel<CommentViewModel> {
        parametersOf(id, entryId)
    }

    private val sectionsPagerAdapter by unsafeLazy { SectionsPagerAdapter(supportFragmentManager) }

    private val viewPager: ViewPager by bindView(R.id.viewPager)
    private val tabs: TabLayout by bindView(R.id.tabs)

    private var tabLayoutHelper: TabLayoutHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupToolbar()

        viewPager.adapter = sectionsPagerAdapter

        tabLayoutHelper = TabLayoutHelper(tabs, viewPager).apply { isAutoAdjustTabModeEnabled = true }

        viewModel.isUpdate.observe(this, Observer {
            title = getString(
                when (it) {
                    true -> R.string.action_update_comment
                    false -> R.string.action_create_comment
                }
            )
        })

        viewModel.publishResult.observe(this, Observer {
            if (it != null) {
                toast(R.string.fragment_comment_published)

                finish()
            }
        })

        viewModel.publishError.observe(this, Observer {
            it?.let {
                multilineSnackbar(
                    getString(R.string.error_comment_publish, getString(it.message)),
                    Snackbar.LENGTH_LONG, it.buttonMessage, it.toClickListener(this)
                )
            }
        })
    }

    override fun onDestroy() {
        tabLayoutHelper?.release()
        tabLayoutHelper = null
        viewPager.adapter = null

        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        IconicsMenuInflaterUtil.inflate(menuInflater, this, R.menu.activity_comment, menu, true)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.action_publish -> viewModel.publish()
        }

        return super.onOptionsItemSelected(item)
    }

    private fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.subtitle = name?.trim()
    }

    inner class SectionsPagerAdapter(fragmentManager: FragmentManager) : FragmentPagerAdapter(
        fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
    ) {

        override fun getItem(position: Int) = when (position) {
            0 -> CommentEditFragment.newInstance()
            1 -> CommentPreviewFragment.newInstance()
            else -> error("Unknown index passed: $position")
        }

        override fun getCount() = 2

        override fun getPageTitle(position: Int): String = when (position) {
            0 -> getString(R.string.fragment_comment_edit)
            1 -> getString(R.string.fragment_comment_preview)
            else -> error("Unknown index passed: $position")
        }
    }
}
