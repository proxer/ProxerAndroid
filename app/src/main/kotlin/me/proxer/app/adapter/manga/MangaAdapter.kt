package me.proxer.app.adapter.manga

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.animation.GlideAnimation
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.target.Target
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import me.proxer.app.R
import me.proxer.app.adapter.base.PagingAdapter
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.extension.bindView
import me.proxer.app.util.extension.decodedName
import me.proxer.library.entitiy.manga.Page
import me.proxer.library.util.ProxerUrls
import java.io.File

/**
 * @author Ruben Gees
 */
class MangaAdapter(private val glide: RequestManager) : PagingAdapter<Page>() {

    private lateinit var server: String
    private lateinit var entryId: String
    private lateinit var id: String

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagingViewHolder<Page> {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_manga_page, parent, false))
    }

    fun init(server: String, entryId: String, id: String) {
        this.server = server
        this.entryId = entryId
        this.id = id
    }

    inner class ViewHolder(itemView: View) : PagingViewHolder<Page>(itemView) {

        private var target: Target<File>? = null
        private val image: SubsamplingScaleImageView by bindView(R.id.image)

        init {
            image.setDoubleTapZoomDuration(200)

            // Make scrolling smoother by hacking the SubsamplingScaleImageView to only receive touch events
            // when zooming.
            image.setOnTouchListener { _, event ->
                val shouldInterceptEvent = event.action == MotionEvent.ACTION_MOVE && event.pointerCount == 1 &&
                        image.scale == image.minScale

                if (shouldInterceptEvent) {
                    image.parent.requestDisallowInterceptTouchEvent(true)
                    itemView.onTouchEvent(event)
                    image.parent.requestDisallowInterceptTouchEvent(false)

                    true
                } else {
                    false
                }
            }
        }

        override fun bind(item: Page) {
            val width = DeviceUtils.getScreenWidth(image.context)
            val height = (item.height * width.toFloat() / item.width.toFloat()).toInt()

            image.recycle()
            image.layoutParams.height = height

            target?.let {
                Glide.clear(it)
            }

            target = object : SimpleTarget<File>() {
                override fun onResourceReady(resource: File, glideAnimation: GlideAnimation<in File>?) {
                    val animationTime = image.context.resources.getInteger(android.R.integer.config_mediumAnimTime)

                    image.setImage(ImageSource.uri(resource.path))
                    image.apply { alpha = 0.2f }.animate().alpha(1.0f)
                            .setDuration(animationTime.toLong()).start()
                }
            }

            glide.load(ProxerUrls.mangaPageImage(server, entryId, id, item.decodedName).toString())
                    .downloadOnly(target)
        }
    }
}

