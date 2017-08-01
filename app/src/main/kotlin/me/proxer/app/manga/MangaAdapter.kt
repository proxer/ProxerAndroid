package me.proxer.app.manga

import android.graphics.PointF
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseAdapter
import me.proxer.app.manga.MangaAdapter.ViewHolder
import me.proxer.app.manga.MangaPageSingle.Input
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.extension.decodedName
import me.proxer.library.entitiy.manga.Page

/**
 * @author Ruben Gees
 */
class MangaAdapter : BaseAdapter<Page, ViewHolder>() {

    lateinit var server: String
    lateinit var entryId: String
    lateinit var id: String
    var isLocal: Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_manga_page, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(data[position])

    override fun onViewRecycled(holder: ViewHolder) {
        holder.image.recycle()

        holder.image.tag.let {
            if (it is Disposable) {
                it.dispose()
            }
        }

        holder.image.tag = null
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val shortAnimationTime = itemView.context.resources.getInteger(android.R.integer.config_shortAnimTime)
        private val mediumAnimationTime = itemView.context.resources.getInteger(android.R.integer.config_mediumAnimTime)

        internal val image: SubsamplingScaleImageView by bindView(R.id.image)

        init {
            image.setDoubleTapZoomDuration(shortAnimationTime)

            applySmoothScrollHack()
        }

        fun bind(item: Page) {
            val width = DeviceUtils.getScreenWidth(image.context)
            val height = (item.height * width.toFloat() / item.width.toFloat()).toInt()
            val scale = width.toFloat() / item.width.toFloat() * 2f

            image.setDoubleTapZoomScale(scale)
            image.layoutParams.height = height
            image.maxScale = scale
            image.tag = MangaPageSingle(image.context, isLocal, Input(server, entryId, id, item.decodedName))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        image.setImage(ImageSource.uri(it.path))
                        image.setScaleAndCenter(0.2f, PointF(0f, 0f))
                        image.apply { alpha = 0.2f }
                                .animate()
                                .alpha(1.0f)
                                .setDuration(mediumAnimationTime.toLong())
                                .start()
                    }, {
                        // Ignore errors.
                    })
        }

        /**
         * Make scrolling smoother by hacking the SubsamplingScaleImageView to only receive touch events when zooming.
         */
        private fun applySmoothScrollHack() {
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
    }
}

