package me.proxer.app.activity

import android.app.Activity
import android.os.Bundle
import android.support.v4.view.ViewCompat
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.animation.GlideAnimation
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget
import me.proxer.app.R
import me.proxer.app.activity.base.MainActivity
import me.proxer.app.util.ActivityUtils
import me.proxer.app.util.extension.bindView
import okhttp3.HttpUrl
import org.jetbrains.anko.intentFor

/**
 * @author Ruben Gees
 */
class ImageDetailActivity : MainActivity() {

    companion object {
        private const val URL_EXTRA = "url"

        fun navigateTo(context: Activity, url: HttpUrl, imageView: ImageView? = null) {
            context.intentFor<ImageDetailActivity>(URL_EXTRA to url.toString()).let {
                ActivityUtils.navigateToWithImageTransition(it, context, imageView)
            }
        }
    }

    private val url: String
        get() = intent.getStringExtra(URL_EXTRA)

    private val root: ViewGroup by bindView(R.id.root)
    private val image: ImageView by bindView(R.id.image)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_image_detail)
        supportPostponeEnterTransition()

        ViewCompat.setTransitionName(image, ActivityUtils.getTransitionName(this))

        Glide.with(this)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .into(object : GlideDrawableImageViewTarget(image) {
                    override fun onResourceReady(resource: GlideDrawable?,
                                                 animation: GlideAnimation<in GlideDrawable>?) {
                        super.onResourceReady(resource, animation)

                        supportStartPostponedEnterTransition()
                    }
                })

        root.setOnClickListener {
            supportFinishAfterTransition()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.home -> {
                supportFinishAfterTransition()

                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }
}
