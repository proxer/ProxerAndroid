package me.proxer.app.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CollapsingToolbarLayout
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.app.ShareCompat
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.h6ah4i.android.tablayouthelper.TabLayoutHelper
import me.proxer.app.R
import me.proxer.app.fragment.media.MediaInfoFragment
import me.proxer.app.util.extension.bindView
import me.proxer.library.enums.Category
import me.proxer.library.util.ProxerUrls
import org.jetbrains.anko.applyRecursively
import org.jetbrains.anko.intentFor

/**
 * @author Ruben Gees
 */
class MediaActivity : MainActivity() {

    companion object {
        private const val ID_EXTRA = "id"
        private const val NAME_EXTRA = "name"
        private const val CATEGORY_EXTRA = "category"

        private const val COMMENTS_SUB_SECTION = "comments"
        private const val EPISODES_SUB_SECTION = "episodes"
        private const val RELATIONS_SUB_SECTION = "relation"

        fun navigateTo(context: Activity, id: String, name: String? = null, category: Category = Category.ANIME) {
            context.startActivity(context.intentFor<MediaActivity>(
                    ID_EXTRA to id,
                    NAME_EXTRA to name,
                    CATEGORY_EXTRA to category)
            )
        }
    }

    val id: String
        get() = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data.pathSegments.getOrElse(1, { "-1" })
            else -> intent.getStringExtra(ID_EXTRA)
        }

    var name: String?
        get() = intent.getStringExtra(NAME_EXTRA)
        set(value) {
            intent.putExtra(NAME_EXTRA, value)

            title = value
        }

    var category: Category
        get() = intent.getSerializableExtra(CATEGORY_EXTRA) as Category
        set(value) {
            intent.putExtra(CATEGORY_EXTRA, category)

//            sectionsPagerAdapter.updateEpisodesTitle(value)
        }

    private val itemToDisplay: Int
        get() = when (intent.action) {
            Intent.ACTION_VIEW -> when (intent.data.pathSegments.getOrNull(2)) {
                COMMENTS_SUB_SECTION -> 1
                EPISODES_SUB_SECTION -> 2
                RELATIONS_SUB_SECTION -> 3
                else -> 0
            }
            else -> 0
        }

    private var sectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)

    private val toolbar: Toolbar by bindView(R.id.toolbar)
    private val appbar: AppBarLayout by bindView(R.id.appbar)
    private val collapsingToolbar: CollapsingToolbarLayout by bindView(R.id.collapsingToolbar)
    private val viewPager: ViewPager by bindView(R.id.viewPager)
    private val coverImage: ImageView by bindView(R.id.image)
    private val tabs: TabLayout by bindView(R.id.tabs)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_image_tabs)
        setSupportActionBar(toolbar)

        setupToolbar()
        setupImage()

        if (savedInstanceState == null) {
            viewPager.currentItem = itemToDisplay
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_media, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_share -> {
                ShareCompat.IntentBuilder
                        .from(this)
                        .setText("https://proxer.me/info/$id")
                        .setType("text/plain")
                        .setChooserTitle(getString(R.string.share_title))
                        .startChooser()

                return true
            }
            android.R.id.home -> {
                finish()

                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun setupImage() {
        coverImage.setOnClickListener {
            if (coverImage.drawable != null) {
                ImageDetailActivity.navigateTo(this@MediaActivity, it as ImageView, ProxerUrls.entryImage(id))
            }
        }

        Glide.with(this)
                .load(ProxerUrls.entryImage(id).toString())
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .into(coverImage)
    }

    private fun setupToolbar() {
        viewPager.adapter = sectionsPagerAdapter

        title = name
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        collapsingToolbar.isTitleEnabled = false

        appbar.addOnOffsetChangedListener { _, verticalOffset ->
            val shadowNeeded = collapsingToolbar.height + verticalOffset >
                    collapsingToolbar.scrimVisibleHeightTrigger

            listOf(tabs, toolbar).forEach {
                it.applyRecursively {
                    if (it is TextView) {
                        when (shadowNeeded) {
                            true -> it.setShadowLayer(3f, 0f, 0f, ContextCompat.getColor(this, android.R.color.black))
                            false -> it.setShadowLayer(0f, 0f, 0f, 0)
                        }
                    }
                }
            }
        }

        TabLayoutHelper(tabs, viewPager).apply { isAutoAdjustTabModeEnabled = true }
    }

    inner class SectionsPagerAdapter(fragmentManager: FragmentManager) :
            FragmentPagerAdapter(fragmentManager) {

        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> MediaInfoFragment.newInstance()
//                1 -> CommentFragment.newInstance()
//                2 -> EpisodesFragment.newInstance()
//                3 -> RelationsFragment.newInstance()
                else -> throw RuntimeException("Unknown index passed")
            }
        }

        override fun getCount() = 1 // 4

        override fun getPageTitle(position: Int): CharSequence? {
            return when (position) {
                0 -> getString(R.string.section_media_info)
//                1 -> getString(R.string.fragment_comments_title)
//                2 -> ParameterMapper.categoryEpisodesTitle(this@MediaActivity, category)
//                3 -> getString(R.string.fragment_relations_title)
                else -> throw RuntimeException("Unknown index passed")
            }
        }

//        fun updateEpisodesTitle(category: String) {
//            tabs.getTabAt(2)?.text = ParameterMapper.categoryEpisodesTitle(this@MediaActivity, category)
//        }
    }
}
