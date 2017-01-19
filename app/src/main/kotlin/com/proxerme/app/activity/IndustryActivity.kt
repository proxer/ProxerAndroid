package com.proxerme.app.activity

import android.app.Activity
import android.os.Bundle
import android.support.design.widget.CollapsingToolbarLayout
import android.support.v4.app.ShareCompat
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.proxerme.app.R
import com.proxerme.app.fragment.info.IndustryInfoFragment
import com.proxerme.app.util.androidUri
import com.proxerme.app.util.bindView
import com.proxerme.library.info.ProxerUrlHolder
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

    private val id: String
        get() = intent.getStringExtra(EXTRA_ID)

    private var name: String?
        get() = intent.getStringExtra(EXTRA_NAME)
        set(value) {
            intent.putExtra(EXTRA_NAME, value)
        }

    private val toolbar: Toolbar by bindView(R.id.toolbar)
    private val collapsingToolbar: CollapsingToolbarLayout by bindView(R.id.collapsingToolbar)
    private val image: ImageView by bindView(R.id.image)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_translator_group)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        collapsingToolbar.isTitleEnabled = false
        title = name

        image.setOnClickListener {
            ImageDetailActivity.navigateTo(this@IndustryActivity, it as ImageView,
                    ProxerUrlHolder.getIndustryImageUrl(id))
        }

        loadImage()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().replace(R.id.container,
                    IndustryInfoFragment.newInstance(id)).commitNow()
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

    fun updateName(newName: String) {
        name = newName
        title = newName
    }

    private fun loadImage() {
        Glide.with(this)
                .load(ProxerUrlHolder.getIndustryImageUrl(id).androidUri())
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .into(image)
    }
}