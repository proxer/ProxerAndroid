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
import com.proxerme.app.fragment.info.TranslatorGroupInfoFragment
import com.proxerme.app.fragment.info.TranslatorGroupProjectsFragment
import com.proxerme.app.util.androidUri
import com.proxerme.app.util.bindView
import com.proxerme.library.info.ProxerUrlHolder
import org.jetbrains.anko.applyRecursively
import org.jetbrains.anko.intentFor

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class TranslatorGroupActivity : MainActivity() {

    companion object {
        private const val EXTRA_ID = "extra_id"
        private const val EXTRA_NAME = "extra_name"

        fun navigateTo(context: Activity, id: String, name: String? = null) {
            context.startActivity(context.intentFor<TranslatorGroupActivity>(
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

        setContentView(R.layout.activity_translator_group)
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
                        .setText("https://proxer.me/translatorgroups?id=$id")
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
            ImageDetailActivity.navigateTo(this@TranslatorGroupActivity, it as ImageView,
                    ProxerUrlHolder.getTranslatorGroupImageUrl(id))
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
                            true -> it.setShadowLayer(2f, 0f, 0f,
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
                .load(ProxerUrlHolder.getTranslatorGroupImageUrl(id).androidUri())
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .into(image)
    }

    inner class SectionsPagerAdapter(fragmentManager: FragmentManager) :
            FragmentPagerAdapter(fragmentManager) {

        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> TranslatorGroupInfoFragment.newInstance()
                1 -> TranslatorGroupProjectsFragment.newInstance()
                else -> throw RuntimeException("Unknown index passed")
            }
        }

        override fun getCount() = 2

        override fun getPageTitle(position: Int): CharSequence? {
            return when (position) {
                0 -> getString(R.string.fragment_translator_group_info_title)
                1 -> getString(R.string.fragment_translator_group_projects_title)
                else -> throw RuntimeException("Unknown index passed")
            }
        }
    }
}