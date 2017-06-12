package me.proxer.app.adapter.manga

import android.graphics.PointF
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.rubengees.ktask.base.Task
import com.rubengees.ktask.util.TaskBuilder
import me.proxer.app.R
import me.proxer.app.adapter.base.BaseAdapter
import me.proxer.app.task.manga.MangaPageDownloadTask
import me.proxer.app.task.manga.MangaPageDownloadTask.MangaPageDownloadTaskInput
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.extension.bindView
import me.proxer.app.util.extension.decodedName
import me.proxer.library.entitiy.manga.Page

/**
 * @author Ruben Gees
 */
class MangaAdapter : BaseAdapter<Page>() {

    private lateinit var server: String
    private lateinit var entryId: String
    private lateinit var id: String

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<Page> {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_manga_page, parent, false))
    }

    override fun onViewRecycled(holder: BaseViewHolder<Page>?) {
        if (holder is ViewHolder) {
            holder.image.recycle()
            holder.image.tag?.let {
                if (it is Task<*, *>) {
                    it.destroy()
                }
            }

            holder.image.tag = null
        }
    }

    fun init(server: String, entryId: String, id: String) {
        this.server = server
        this.entryId = entryId
        this.id = id
    }

    internal inner class ViewHolder(itemView: View) : BaseViewHolder<Page>(itemView) {

        private val shortAnimationTime = itemView.context.resources.getInteger(android.R.integer.config_shortAnimTime)
        private val mediumAnimationTime = itemView.context.resources.getInteger(android.R.integer.config_mediumAnimTime)

        internal val image: SubsamplingScaleImageView by bindView(R.id.image)

        init {
            image.setDoubleTapZoomDuration(shortAnimationTime)

            applySmoothScrollHack()
        }

        override fun bind(item: Page) {
            val width = DeviceUtils.getScreenWidth(image.context)
            val height = (item.height * width.toFloat() / item.width.toFloat()).toInt()
            val scale = width.toFloat() / item.width.toFloat() * 2f

            image.recycle()
            image.setDoubleTapZoomScale(scale)
            image.layoutParams.height = height
            image.maxScale = scale
            image.tag = TaskBuilder.task(MangaPageDownloadTask(image.context.filesDir))
                    .async()
                    .onSuccess {
                        image.post {
                            image.setImage(ImageSource.uri(it.path))
                            image.setScaleAndCenter(0.2f, PointF(0f, 0f))
                            image.apply { alpha = 0.2f }
                                    .animate()
                                    .alpha(1.0f)
                                    .setDuration(mediumAnimationTime.toLong())
                                    .start()
                        }
                    }
                    .build()
                    .apply {
                        forceExecute(MangaPageDownloadTaskInput(server, entryId, id, item.decodedName))
                    }
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

