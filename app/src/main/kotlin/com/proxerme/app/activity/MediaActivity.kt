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
import butterknife.bindView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.proxerme.app.R
import com.proxerme.app.fragment.media.MediaInfoFragment
import com.proxerme.library.info.ProxerUrlHolder

class MediaActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_ID = "extra_id"
        private const val EXTRA_NAME = "extra_name"
        private const val STATE_NAME = "activity_media_name"

        fun navigateTo(context: Activity, id: String, name: String? = null) {

            val intent = Intent(context, MediaActivity::class.java)
                    .apply {
                        this.putExtra(EXTRA_ID, id)
                        this.putExtra(EXTRA_NAME, name)
                    }

            context.startActivity(intent)
        }
    }

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

        id = intent.getStringExtra(EXTRA_ID)

        if (savedInstanceState == null) {
            name = intent.getStringExtra(EXTRA_NAME)
        } else {
            name = savedInstanceState.getString(STATE_NAME)
        }

        setupToolbar()
        setupImage()
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
                else -> throw RuntimeException("Unknown index passed")
            }
        }

        override fun getCount() = 1

        override fun getPageTitle(position: Int): CharSequence? {
            return when (position) {
                0 -> "Info"
                else -> throw RuntimeException("Unknown index passed")
            }
        }
    }
}
