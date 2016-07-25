package com.proxerme.app.adapter

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
import com.proxerme.app.util.TimeUtil
import com.proxerme.library.connection.notifications.entitiy.News
import com.proxerme.library.info.ProxerUrlHolder
import java.util.*
import kotlin.comparisons.compareByDescending

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
class NewsAdapter(val savedInstanceState: Bundle?) :
        RecyclerView.Adapter<NewsAdapter.ViewHolder>() {

    private companion object {
        const val STATE_ITEMS = "adapter_news_state_items"
        const val STATE_EXPANDED_IDS = "adapter_news_state_extension_ids"
        const val ICON_SIZE = 32
        const val ICON_PADDING = 8
        const val ROTATION_HALF = 180f
        const val DESCRIPTION_MAX_LINES = 3
    }

    private val list = ArrayList<News>()
    private val expanded = HashMap<String, Boolean>()
    var callback: OnNewsInteractionListener? = null

    init {
        setHasStableIds(true)

        savedInstanceState?.let {
            list.addAll(it.getParcelableArrayList(STATE_ITEMS))
            it.getStringArrayList(STATE_EXPANDED_IDS)
                    .associateByTo(expanded, { it }, { true })
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder? {
        return ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_news, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(list[position])

    override fun getItemCount(): Int = list.count()

    override fun getItemId(position: Int): Long = list[position].id.toLong()

    fun addItems(newItems: Collection<News>) {
        list.addAll(newItems.filter { list.binarySearch(it, compareByDescending { it.time }) < 0 })
        list.sortByDescending { it.time }

        notifyDataSetChanged()
    }

    fun clear() {
        list.clear()

        notifyDataSetChanged()
    }

    fun saveInstanceState(outState: Bundle) {
        outState.putParcelableArrayList(STATE_ITEMS, list)
        outState.putStringArrayList(STATE_EXPANDED_IDS, ArrayList(expanded.keys))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val expand: ImageButton by bindView(R.id.expand)
        private val description: TextView by bindView(R.id.description)
        private val image: ImageView by bindView(R.id.image)
        private val title: TextView by bindView(R.id.title)
        private val category: TextView by bindView(R.id.category)
        private val time: TextView by bindView(R.id.time)

        init {
            expand.setImageDrawable(IconicsDrawable(expand.context)
                    .colorRes(R.color.icons_grey)
                    .sizeDp(ICON_SIZE)
                    .paddingDp(ICON_PADDING)
                    .icon(CommunityMaterial.Icon.cmd_chevron_down))

            expand.setOnClickListener {
                val id = list[adapterPosition].id

                if (expanded.containsKey(id)) {
                    expanded.remove(id)

                    description.maxLines = DESCRIPTION_MAX_LINES
                    ViewCompat.animate(expand).rotation(0f)
                } else {
                    expanded.put(id, true)

                    description.maxLines = Integer.MAX_VALUE
                    ViewCompat.animate(expand).rotation(ROTATION_HALF)
                }

                callback?.onNewsExpanded(it, list[adapterPosition])
            }

            itemView.setOnClickListener {
                callback?.onNewsClick(it, list[adapterPosition])
            }

            image.setOnClickListener {
                callback?.onNewsImageClick(it, list[adapterPosition])
            }
        }

        fun bind(item: News) {
            Glide.with(image.context)
                    .load(ProxerUrlHolder.getNewsImageUrl(item.id, item.imageId))
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(image)

            title.text = item.subject
            description.text = item.description
            category.text = item.categoryTitle
            time.text = TimeUtil.convertToRelativeReadableTime(time.context,
                    item.time)

            if (expanded.containsKey(item.id)) {
                description.maxLines = Integer.MAX_VALUE
                ViewCompat.setRotation(expand, ROTATION_HALF)
            } else {
                description.maxLines = DESCRIPTION_MAX_LINES
                ViewCompat.setRotation(expand, 0f)
            }
        }
    }

    abstract class OnNewsInteractionListener {
        open fun onNewsClick(v: View, news: News) {

        }

        open fun onNewsImageClick(v: View, news: News) {

        }

        open fun onNewsExpanded(v: View, news: News) {

        }
    }
}