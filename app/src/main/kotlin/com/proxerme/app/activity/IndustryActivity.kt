package com.proxerme.app.activity

import android.app.Activity
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
import com.proxerme.app.fragment.info.IndustryInfoFragment
import com.proxerme.app.fragment.info.IndustryProjectsFragment
import com.proxerme.app.util.bindView
import com.proxerme.app.util.extension.androidUri
import com.proxerme.library.info.ProxerUrlHolder
import org.jetbrains.anko.applyRecursively
import org.jetbrains.anko.intentFor

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class IndustryActivity : MainActivity() {

    companion object {
        private const val EXTRA_ID = "extra_id"
        private const val EXTRA_NAME = "extra_name"

        fun navigateTo(context: Activity, id: String, name: String? = null) {
            context.startActivity(context.intentFor<IndustryActivity>(
                    EXTRA_ID to id,
                    EXTRA_NAME to name)
            )
        }
    }

    val id: String
        get() = intent.getStringExtra(EXTRA_ID)

    var name: String?
        get() = intent.getStringExtra(EXTRA_NAME)
        set(value) {
            intent.putExtra(EXTRA_NAME, value)

            title = value
        }

    private val sectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)

    private val toolbar: Toolbar by bindView(R.id.toolbar)
    private val appbar: AppBarLayout by bindView(R.id.appbar)
    private val collapsingToolbar: CollapsingToolbarLayout by bindView(R.id.collapsingToolbar)
    private val viewPager: ViewPager by bindView(R.id.viewPager)
    private val image: ImageView by bindView(R.id.image)
    private val tabs: TabLayout by bindView(R.id.tabs)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_industry)
        setSupportActionBar(toolbar)

        setupToolbar()
        setupImage()

        if (savedInstanceState == null) {
            viewPager.currentItem = 0
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_industry, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_share -> {
                ShareCompat.IntentBuilder
                        .from(this)
                        .setText("https://proxer.me/industry?id=$id")
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
        image.setOnClickListener {
            ImageDetailActivity.navigateTo(this@IndustryActivity, it as ImageView,
                    ProxerUrlHolder.getIndustryImageUrl(id))
        }

        loadImage()
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
                            true -> it.setShadowLayer(1f, 0f, 0f,
                                    ContextCompat.getColor(this, R.color.md_black_1000))
                            false -> it.setShadowLayer(0f, 0f, 0f, 0)
                        }
                    }
                }
            }
        }

        TabLayoutHelper(tabs, viewPager).apply { isAutoAdjustTabModeEnabled = true }
    }

    private fun loadImage() {
        Glide.with(this)
                .load(ProxerUrlHolder.getIndustryImageUrl(id).androidUri())
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .into(image)
    }

    inner class SectionsPagerAdapter(fragmentManager: FragmentManager) :
            FragmentPagerAdapter(fragmentManager) {

        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> IndustryInfoFragment.newInstance()
                1 -> IndustryProjectsFragment.newInstance()
                else -> throw RuntimeException("Unknown index passed")
            }
        }

        override fun getCount() = 2

        override fun getPageTitle(position: Int): CharSequence? {
            return when (position) {
                0 -> getString(R.string.fragment_industry_info_title)
                1 -> getString(R.string.fragment_industry_projects_title)
                else -> throw RuntimeException("Unknown index passed")
            }
        }
    }
}
