package me.proxer.app.adapter.news

import android.support.v4.view.ViewCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import me.proxer.app.R
import me.proxer.app.adapter.base.PagingAdapter
import me.proxer.app.util.TimeUtils
import me.proxer.app.util.extension.bindView
import me.proxer.library.entitiy.notifications.NewsArticle
import me.proxer.library.util.ProxerUrls
import java.util.*

/**
 * @author Ruben Gees
 */
class NewsArticleAdapter : PagingAdapter<NewsArticle>() {

    private val expansionMap = HashMap<String, Boolean>()

    var callback: NewsAdapterCallback? = null

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder? {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_news, parent, false))
    }

    override fun areContentsTheSame(oldItem: NewsArticle, newItem: NewsArticle): Boolean {
        return oldItem.date == newItem.date && oldItem.category == newItem.category &&
                oldItem.image == newItem.image && oldItem.subject == newItem.subject &&
                oldItem.description == newItem.description
    }

    override fun destroy() {
        super.destroy()

        callback = null
    }

    inner class ViewHolder(itemView: View) : PagingViewHolder<NewsArticle>(itemView) {

        private val expand: ImageButton by bindView(R.id.expand)
        private val description: TextView by bindView(R.id.description)
        private val image: ImageView by bindView(R.id.image)
        private val title: TextView by bindView(R.id.title)
        private val category: TextView by bindView(R.id.category)
        private val time: TextView by bindView(R.id.time)

        init {
            itemView.setOnClickListener {
                withSafeAdapterPosition {
                    callback?.onNewsArticleClick(internalList[it])
                }
            }

            expand.setImageDrawable(IconicsDrawable(expand.context)
                    .colorRes(R.color.icon)
                    .sizeDp(32)
                    .paddingDp(8)
                    .icon(CommunityMaterial.Icon.cmd_chevron_down))

            expand.setOnClickListener {
                withSafeAdapterPosition {
                    val id = internalList[it].id

                    if (expansionMap.containsKey(id)) {
                        expansionMap.remove(id)
                    } else {
                        expansionMap.put(id, true)

                        callback?.onNewsArticleExpansion(internalList[it])
                    }

                    notifyItemChanged(it)
                }
            }

            image.setOnClickListener { view ->
                withSafeAdapterPosition {
                    callback?.onNewsArticleImageClick(view as ImageView, internalList[it])
                }
            }
        }

        override fun bind(item: NewsArticle) {
            title.text = item.subject
            description.text = item.description.trim()
            category.text = item.category
            time.text = TimeUtils.convertToRelativeReadableTime(time.context, item.date)

            if (expansionMap.containsKey(item.id)) {
                description.maxLines = Int.MAX_VALUE

                ViewCompat.animate(expand).rotation(180f)
            } else {
                description.maxLines = 3

                ViewCompat.animate(expand).rotation(0f)
            }

            description.post {
                if (description.lineCount <= 3) {
                    expand.visibility = View.GONE
                } else {
                    expand.visibility = View.VISIBLE
                }
            }

            Glide.with(image.context)
                    .load(ProxerUrls.newsImage(item.id, item.image).toString())
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(image)
        }
    }

    interface NewsAdapterCallback {
        fun onNewsArticleClick(item: NewsArticle) {}
        fun onNewsArticleImageClick(view: ImageView, item: NewsArticle) {}
        fun onNewsArticleExpansion(item: NewsArticle) {}
    }
}
