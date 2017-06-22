package me.proxer.app.adapter.news

import android.os.Bundle
import android.support.v4.view.ViewCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import me.proxer.app.R
import me.proxer.app.adapter.base.BaseGlideAdapter
import me.proxer.app.application.GlideRequests
import me.proxer.app.util.TimeUtils
import me.proxer.app.util.data.ParcelableStringBooleanMap
import me.proxer.app.util.extension.bindView
import me.proxer.library.entitiy.notifications.NewsArticle
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
class NewsArticleAdapter(savedInstanceState: Bundle?, glide: GlideRequests) : BaseGlideAdapter<NewsArticle>(glide) {

    private companion object {
        private const val EXPANDED_STATE = "news_expanded"
    }

    private val expanded: ParcelableStringBooleanMap

    var callback: NewsAdapterCallback? = null

    init {
        expanded = when (savedInstanceState) {
            null -> ParcelableStringBooleanMap()
            else -> savedInstanceState.getParcelable(EXPANDED_STATE)
        }

        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<NewsArticle> {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_news, parent, false))
    }

    override fun onViewRecycled(holder: BaseViewHolder<NewsArticle>) {
        if (holder is ViewHolder) {
            clearImage(holder.image)
        }
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

    fun saveInstanceState(outState: Bundle) {
        outState.putParcelable(EXPANDED_STATE, expanded)
    }

    internal inner class ViewHolder(itemView: View) : BaseViewHolder<NewsArticle>(itemView) {

        internal val expand: ImageButton by bindView(R.id.expand)
        internal val description: TextView by bindView(R.id.description)
        internal val image: ImageView by bindView(R.id.image)
        internal val title: TextView by bindView(R.id.title)
        internal val category: TextView by bindView(R.id.category)
        internal val time: TextView by bindView(R.id.time)

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

                    if (expanded[id] ?: false) {
                        expanded.remove(id)
                    } else {
                        expanded.put(id, true)

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
            ViewCompat.setTransitionName(image, "news_${item.id}")

            title.text = item.subject
            description.text = item.description.trim()
            category.text = item.category
            time.text = TimeUtils.convertToRelativeReadableTime(time.context, item.date)

            if (expanded[item.id] ?: false) {
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

            loadImage(image, ProxerUrls.newsImage(item.id, item.image))
        }
    }

    interface NewsAdapterCallback {
        fun onNewsArticleClick(item: NewsArticle) {}
        fun onNewsArticleImageClick(view: ImageView, item: NewsArticle) {}
        fun onNewsArticleExpansion(item: NewsArticle) {}
    }
}
