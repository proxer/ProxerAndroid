package com.proxerme.app.activity

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
import com.proxerme.app.R
import com.proxerme.app.fragment.media.CommentFragment
import com.proxerme.app.fragment.media.EpisodesFragment
import com.proxerme.app.fragment.media.MediaInfoFragment
import com.proxerme.app.fragment.media.RelationsFragment
import com.proxerme.app.util.bindView
import com.proxerme.library.info.ProxerUrlHolder
import org.jetbrains.anko.applyRecursively
import org.jetbrains.anko.intentFor

class MediaActivity : MainActivity() {

    companion object {
        private const val EXTRA_ID = "extra_id"
        private const val EXTRA_NAME = "extra_name"

        private const val SECTION_COMMENTS = "comments"
        private const val SECTION_EPISODES = "episodes"
        private const val SECTION_RELATIONS = "relation"

        fun navigateTo(context: Activity, id: String, name: String? = null) {
            context.startActivity(context.intentFor<MediaActivity>(
                    EXTRA_ID to id,
                    EXTRA_NAME to name)
            )
        }
    }

    val id: String
        get() = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data.pathSegments.getOrElse(1, { "-1" })
            else -> intent.getStringExtra(EXTRA_ID)
        }

    var name: String?
        get() = intent.getStringExtra(EXTRA_NAME)
        set(value) {
            intent.putExtra(EXTRA_NAME, value)

            title = value
        }

    private val itemToDisplay: Int
        get() = when (intent.action) {
            Intent.ACTION_VIEW -> when (intent.data.pathSegments.getOrNull(2)) {
                SECTION_COMMENTS -> 1
                SECTION_EPISODES -> 2
                SECTION_RELATIONS -> 3
                else -> 0
            }
            else -> 0
        }

    private var sectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)

    private val toolbar: Toolbar by bindView(R.id.toolbar)
    private val appbar: AppBarLayout by bindView(R.id.appbar)
    private val collapsingToolbar: CollapsingToolbarLayout by bindView(R.id.collapsingToolbar)
    private val viewPager: ViewPager by bindView(R.id.viewPager)
    private val coverImage: ImageView by bindView(R.id.coverImage)
    private val tabs: TabLayout by bindView(R.id.tabs)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_media)
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
            ImageDetailActivity.navigateTo(this@MediaActivity, it as ImageView,
                    ProxerUrlHolder.getCoverImageUrl(id))
        }

        Glide.with(this)
                .load(ProxerUrlHolder.getCoverImageUrl(id).toString())
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
                            true -> it.setShadowLayer(1f, 1f, 1f,
                                    ContextCompat.getColor(this, R.color.md_black_1000))
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
                1 -> CommentFragment.newInstance()
                2 -> EpisodesFragment.newInstance()
                3 -> RelationsFragment.newInstance()
                else -> throw RuntimeException("Unknown index passed")
            }
        }

        override fun getCount() = 4

        override fun getPageTitle(position: Int): CharSequence? {
            return when (position) {
                0 -> getString(R.string.fragment_media_info_title)
                1 -> getString(R.string.fragment_comments_title)
                2 -> getString(R.string.fragment_episodes_title)
                3 -> getString(R.string.fragment_relations_title)
                else -> throw RuntimeException("Unknown index passed")
            }
        }
    }
}
