package me.proxer.app.base

import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.transition.Transition
import android.view.MenuItem
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.core.view.ViewCompat
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.target.ImageViewTarget
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.tabs.TabLayout
import com.h6ah4i.android.tablayouthelper.TabLayoutHelper
import com.jakewharton.rxbinding2.support.design.widget.offsetChanges
import com.jakewharton.rxbinding2.view.clicks
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.kotlin.autoDisposable
import kotterknife.bindView
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.ui.ImageDetailActivity
import me.proxer.app.util.ActivityUtils
import me.proxer.app.util.wrapper.SimpleGlideRequestListener
import okhttp3.HttpUrl

/**
 * @author Ruben Gees
 */
abstract class ImageTabsActivity : DrawerActivity() {

    abstract val headerImageUrl: HttpUrl?
    abstract val sectionsPagerAdapter: PagerAdapter

    override val contentView
        get() = R.layout.activity_image_tabs

    protected open val itemToDisplay
        get() = 0

    private var isHeaderImageVisible = true

    protected open val collapsingToolbar: CollapsingToolbarLayout by bindView(R.id.collapsingToolbar)
    protected open val viewPager: ViewPager by bindView(R.id.viewPager)
    protected open val headerImage: ImageView by bindView(R.id.image)
    protected open val tabs: TabLayout by bindView(R.id.tabs)

    private var tabLayoutHelper: TabLayoutHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupToolbar()
        setupImage()
        loadImage()

        if (isTransitionPossible(savedInstanceState) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            supportPostponeEnterTransition()

            window.sharedElementEnterTransition.addListener(object : TransitionListenerWrapper {
                override fun onTransitionEnd(transition: Transition?) {
                    window.sharedElementEnterTransition.removeListener(this)

                    viewPager.postDelayed({ setupContent(savedInstanceState) }, 50)
                }
            })
        } else {
            setupContent(savedInstanceState)
        }
    }

    override fun onDestroy() {
        tabLayoutHelper?.release()
        tabLayoutHelper = null

        super.onDestroy()
    }

    override fun onBackPressed() = when (isHeaderImageVisible && headerImage.drawable != null) {
        true -> super.onBackPressed()
        false -> finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> when (isHeaderImageVisible && headerImage.drawable != null) {
                true -> supportFinishAfterTransition()
                false -> finish()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    protected open fun setupImage() {
        ViewCompat.setTransitionName(headerImage, ActivityUtils.getTransitionName(this))

        headerImage.clicks()
            .autoDisposable(this.scope())
            .subscribe {
                val safeHeaderImageUrl = headerImageUrl

                if (headerImage.drawable != null && safeHeaderImageUrl != null) {
                    if (ViewCompat.getTransitionName(headerImage) == null) {
                        ViewCompat.setTransitionName(headerImage, "header")
                    }

                    ImageDetailActivity.navigateTo(this, safeHeaderImageUrl, headerImage)
                }
            }
    }

    protected open fun loadImage(animate: Boolean = true) {
        if (headerImageUrl == null) {
            loadEmptyImage()

            supportStartPostponedEnterTransition()
        } else {
            GlideApp.with(this)
                .load(headerImageUrl.toString())
                .listener(object : SimpleGlideRequestListener<Drawable?> {
                    override fun onLoadFailed(error: GlideException?): Boolean {
                        supportStartPostponedEnterTransition()

                        return false
                    }
                })
                .into(object : ImageViewTarget<Drawable>(headerImage) {
                    override fun setResource(resource: Drawable?) {
                        headerImage.setImageDrawable(resource)

                        if (resource != null) {
                            supportStartPostponedEnterTransition()
                        }
                    }
                })
        }
    }

    protected open fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        collapsingToolbar.isTitleEnabled = false

        appbar.offsetChanges()
            .autoDisposable(this.scope())
            .subscribe {
                isHeaderImageVisible = collapsingToolbar.height + it > collapsingToolbar.scrimVisibleHeightTrigger
            }
    }

    protected open fun setupContent(savedInstanceState: Bundle?) {
        viewPager.adapter = sectionsPagerAdapter

        if (savedInstanceState == null) {
            viewPager.currentItem = itemToDisplay
        }

        tabLayoutHelper = TabLayoutHelper(tabs, viewPager).apply { isAutoAdjustTabModeEnabled = true }
    }

    protected open fun loadEmptyImage() {}

    private fun isTransitionPossible(savedInstanceState: Bundle?): Boolean {
        return savedInstanceState == null && ActivityUtils.getTransitionName(this) != null
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    interface TransitionListenerWrapper : Transition.TransitionListener {
        override fun onTransitionEnd(transition: Transition?) {}
        override fun onTransitionResume(transition: Transition?) {}
        override fun onTransitionPause(transition: Transition?) {}
        override fun onTransitionCancel(transition: Transition?) {}
        override fun onTransitionStart(transition: Transition?) {}
    }
}
