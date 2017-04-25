package me.proxer.app.adapter.info

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import me.proxer.app.R
import me.proxer.app.adapter.base.PagingAdapter
import me.proxer.app.util.extension.bindView
import me.proxer.app.util.extension.toAppString
import me.proxer.library.entitiy.list.TranslatorGroupProject
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
class TranslatorGroupProjectAdapter(private val glide: RequestManager) : PagingAdapter<TranslatorGroupProject>() {

    var callback: TranslatorGroupProjectAdapterCallback? = null

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_project, parent, false))
    }

    override fun destroy() {
        super.destroy()

        callback = null
        glide.onDestroy()
    }

    inner class ViewHolder(itemView: View) : PagingViewHolder<TranslatorGroupProject>(itemView) {

        private val title: TextView by bindView(R.id.title)
        private val medium: TextView by bindView(R.id.medium)
        private val image: ImageView by bindView(R.id.image)
        private val ratingContainer: ViewGroup by bindView(R.id.ratingContainer)
        private val rating: RatingBar by bindView(R.id.rating)
        private val status: TextView by bindView(R.id.status)

        init {
            itemView.setOnClickListener { view ->
                withSafeAdapterPosition {
                    callback?.onProjectClick(view, internalList[it])
                }
            }
        }

        override fun bind(item: TranslatorGroupProject) {
            title.text = item.name
            medium.text = item.medium.toAppString(medium.context)
            status.text = item.state.toAppString(status.context)

            if (item.rating > 0) {
                ratingContainer.visibility = View.VISIBLE
                rating.rating = item.rating / 2.0f
            } else {
                ratingContainer.visibility = View.GONE
            }

            glide.load(ProxerUrls.entryImage(item.id).toString())
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(image)
        }
    }

    interface TranslatorGroupProjectAdapterCallback {
        fun onProjectClick(view: View, item: TranslatorGroupProject) {}
    }
}