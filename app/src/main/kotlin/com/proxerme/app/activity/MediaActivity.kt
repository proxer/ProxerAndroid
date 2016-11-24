package com.proxerme.app.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.CollapsingToolbarLayout
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.proxerme.app.R
import com.proxerme.app.fragment.media.CommentFragment
import com.proxerme.app.fragment.media.EpisodesFragment
import com.proxerme.app.fragment.media.MediaInfoFragment
import com.proxerme.app.fragment.media.RelationsFragment
import com.proxerme.app.module.CustomTabsModule
import com.proxerme.app.util.bindView
import com.proxerme.library.info.ProxerUrlHolder
import customtabs.CustomTabActivityHelper

class MediaActivity : AppCompatActivity(), CustomTabsModule {

    companion object {
        private const val EXTRA_ID = "extra_id"
        private const val EXTRA_NAME = "extra_name"
        private const val STATE_NAME = "activity_media_name"

        fun navigateTo(context: Activity, id: String, name: String? = null) {
            context.startActivity(Intent(context, MediaActivity::class.java)
                    .apply {
                        this.putExtra(EXTRA_ID, id)
                        this.putExtra(EXTRA_NAME, name)
                    })
        }
    }

    override val customTabActivityHelper: CustomTabActivityHelper = CustomTabActivityHelper()

    private lateinit var id: String
    private var name: String? = null
    private var sectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)

    private val toolbar: Toolbar by bindView(R.id.toolbar)
    private val collapsingToolbar: CollapsingToolbarLayout by bindView(R.id.collapsingToolbar)
    private val viewPager: ViewPager by bindView(R.id.viewPager)
    private val coverImage: ImageView by bindView(R.id.coverImage)
    private val tabs: TabLayout by bindView(R.id.tabs)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_media)
        setSupportActionBar(toolbar)

        id = if (intent.action == Intent.ACTION_VIEW)
            intent.data.pathSegments.getOrElse(1, { "-1" })
        else {
            intent.getStringExtra(EXTRA_ID)
        }

        if (savedInstanceState == null) {
            name = intent.getStringExtra(EXTRA_NAME)
        } else {
            name = savedInstanceState.getString(STATE_NAME)
        }

        setupToolbar()
        setupImage()

        if (savedInstanceState == null) {
            val sectionToShow = if (intent.action == Intent.ACTION_VIEW)
                when (intent.data.pathSegments.getOrNull(2)) {
                    "comments" -> 1
                    "episodes" -> 2
                    "relation" -> 3
                    else -> 0
                }
            else {
                0
            }

            viewPager.currentItem = sectionToShow
        }
    }

    override fun onStart() {
        super.onStart()

        try {
            customTabActivityHelper.bindCustomTabsService(this)
        } catch(ignored: Exception) {
            // Workaround for crash if chrome is not installed
        }
    }

    override fun onStop() {
        customTabActivityHelper.unbindCustomTabsService(this)

        super.onStop()
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(STATE_NAME, name)
    }

    fun setName(name: String) {
        if (this.name == null) {
            this.name = name

            supportActionBar?.title = name
        }
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

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = name
        collapsingToolbar.isTitleEnabled = false
        tabs.setupWithViewPager(viewPager)
    }

    inner class SectionsPagerAdapter(fragmentManager: FragmentManager) :
            FragmentStatePagerAdapter(fragmentManager) {

        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> MediaInfoFragment.newInstance(id)
                1 -> CommentFragment.newInstance(id)
                2 -> EpisodesFragment.newInstance(id)
                3 -> RelationsFragment.newInstance(id)
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
