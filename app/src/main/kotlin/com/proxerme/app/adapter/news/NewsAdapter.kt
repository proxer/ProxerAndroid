package com.proxerme.app.adapter.news

import android.os.Bundle
import android.support.v4.view.ViewCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import butterknife.bindView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.proxerme.app.R
import com.proxerme.app.adapter.framework.PagingAdapter
import com.proxerme.app.util.TimeUtil
import com.proxerme.app.util.Utils
import com.proxerme.app.util.measureAndGetHeight
import com.proxerme.library.connection.notifications.entitiy.News
import com.proxerme.library.info.ProxerUrlHolder
import java.util.*

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
class NewsAdapter(savedInstanceState: Bundle? = null) :
        PagingAdapter<News, NewsAdapter.NewsAdapterCallback>() {

    private companion object {
        private const val ITEMS_STATE = "adapter_news_state_items"
        private const val EXPANDED_IDS_STATE = "adapter_news_state_extension_ids"
        private const val ICON_SIZE = 32
        private const val ICON_PADDING = 8
        private const val ROTATION_HALF = 180f
    }

    private val expanded = HashMap<String, Boolean>()

    init {
        savedInstanceState?.let {
            list.addAll(it.getParcelableArrayList(ITEMS_STATE))
            it.getStringArrayList(EXPANDED_IDS_STATE)
                    .associateByTo(expanded, { it }, { true })
        }

        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = list[position].id.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder? {
        return ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_news, parent, false))
    }

    override fun saveInstanceState(outState: Bundle) {
        outState.putParcelableArrayList(ITEMS_STATE, list)
        outState.putStringArrayList(EXPANDED_IDS_STATE, ArrayList(expanded.keys))
    }

    inner class ViewHolder(itemView: View) : PagingViewHolder<News, NewsAdapterCallback>(itemView) {

        override val adapterList: List<News>
            get() = list
        override val adapterCallback: NewsAdapterCallback?
            get() = callback

        private val expand: ImageButton by bindView(R.id.expand)
        private val description: TextView by bindView(R.id.description)
        private val image: ImageView by bindView(R.id.image)
        private val title: TextView by bindView(R.id.title)
        private val category: TextView by bindView(R.id.category)
        private val time: TextView by bindView(R.id.time)

        init {
            expand.setImageDrawable(IconicsDrawable(expand.context)
                    .colorRes(R.color.icon)
                    .sizeDp(ICON_SIZE)
                    .paddingDp(ICON_PADDING)
                    .icon(CommunityMaterial.Icon.cmd_chevron_down))

            expand.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val id = adapterList[adapterPosition].id

                    if (expanded.containsKey(id)) {
                        expanded.remove(id)
                    } else {
                        expanded.put(id, true)
                    }

                    notifyItemChanged(adapterPosition)
                    adapterCallback?.onNewsExpanded(it, adapterList[adapterPosition])
                }
            }

            image.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    adapterCallback?.onNewsImageClick(it, adapterList[adapterPosition])
                }
            }
        }

        override fun bind(item: News) {
            val maximumHeight = Utils.convertDpToPx(description.context, 88f)

            title.text = item.subject
            description.text = item.description
            description.maxHeight = Int.MAX_VALUE
            category.text = item.categoryTitle
            time.text = TimeUtil.convertToRelativeReadableTime(time.context,
                    item.time)

            if (description.measureAndGetHeight() <= maximumHeight) {
                expand.visibility = View.GONE
            } else {
                expand.visibility = View.VISIBLE
            }

            if (expanded.containsKey(item.id)) {
                description.maxHeight = Int.MAX_VALUE

                ViewCompat.animate(expand).rotation(ROTATION_HALF)
            } else {
                description.maxHeight = maximumHeight

                ViewCompat.animate(expand).rotation(0f)
            }

            Glide.with(image.context)
                    .load(ProxerUrlHolder.getNewsImageUrl(item.id, item.imageId).toString())
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(image)
        }
    }

    abstract class NewsAdapterCallback : PagingAdapter.PagingAdapterCallback<News>() {
        open fun onNewsImageClick(v: View, news: News) {

        }

        open fun onNewsExpanded(v: View, news: News) {

        }
    }
}