package com.proxerme.app.activity

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityOptionsCompat
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.proxerme.app.R
import com.proxerme.app.util.bindView
import okhttp3.HttpUrl

/**
 * An Activity which shows a image with an animation on Lollipop and higher.
 * It also has a transparent background.

 * @author Ruben Gees
 */
class ImageDetailActivity : MainActivity() {

    companion object {
        private const val EXTRA_URL = "extra_url"

        fun navigateTo(context: Activity, image: ImageView,
                       url: HttpUrl) {
            val intent = Intent(context, ImageDetailActivity::class.java)
                    .apply { this.putExtra(EXTRA_URL, url.toString()) }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                context.startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(
                        context, image, image.transitionName).toBundle())
            } else {
                context.startActivity(intent)
            }
        }
    }

    private val root: ViewGroup by bindView(R.id.root)
    private val image: ImageView by bindView(R.id.image)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_detail)

        val url = intent.getStringExtra(EXTRA_URL)

        supportPostponeEnterTransition()
        Glide.with(this)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .listener(object : RequestListener<String, GlideDrawable> {
                    override fun onException(e: Exception, model: String,
                                             target: Target<GlideDrawable>,
                                             isFirstResource: Boolean): Boolean {
                        return false
                    }

                    override fun onResourceReady(resource: GlideDrawable, model: String,
                                                 target: Target<GlideDrawable>,
                                                 isFromMemoryCache: Boolean,
                                                 isFirstResource: Boolean): Boolean {
                        supportStartPostponedEnterTransition()

                        return false
                    }
                }).into(image)

        root.setOnClickListener {
            supportFinishAfterTransition()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                supportFinishAfterTransition()

                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }
}
