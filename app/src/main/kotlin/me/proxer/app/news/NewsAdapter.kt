package me.proxer.app.news

import android.graphics.Color
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
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import io.reactivex.subjects.PublishSubject
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.base.BaseAdapter
import me.proxer.app.util.data.ParcelableStringBooleanMap
import me.proxer.app.util.extension.convertToRelativeReadableTime
import me.proxer.app.util.extension.defaultLoad
import me.proxer.library.entitiy.notifications.NewsArticle
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
class NewsAdapter(savedInstanceState: Bundle?, private val glide: GlideRequests)
    : BaseAdapter<NewsArticle, NewsAdapter.ViewHolder>() {

    private companion object {
        private const val EXPANDED_STATE = "news_expansion_map"
    }

    val clickSubject: PublishSubject<NewsArticle> = PublishSubject.create<NewsArticle>()
    val expansionSubject: PublishSubject<NewsArticle> = PublishSubject.create<NewsArticle>()
    val imageClickSubject: PublishSubject<Pair<ImageView, NewsArticle>> =
            PublishSubject.create<Pair<ImageView, NewsArticle>>()

    private val expansionMap: ParcelableStringBooleanMap

    init {
        expansionMap = when (savedInstanceState) {
            null -> ParcelableStringBooleanMap()
            else -> savedInstanceState.getParcelable(EXPANDED_STATE)
        }

        setHasStableIds(true)
    }

    override fun onViewRecycled(holder: ViewHolder) = glide.clear(holder.image)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_news, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(data[position])

    override fun areContentsTheSame(oldItem: NewsArticle, newItem: NewsArticle) = oldItem.date == newItem.date
            && oldItem.category == newItem.category
            && oldItem.image == newItem.image
            && oldItem.subject == newItem.subject
            && oldItem.description == newItem.description

    override fun saveInstanceState(outState: Bundle) = outState.putParcelable(EXPANDED_STATE, expansionMap)

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal val expand: ImageButton by bindView(R.id.expand)
        internal val description: TextView by bindView(R.id.description)
        internal val image: ImageView by bindView(R.id.image)
        internal val title: TextView by bindView(R.id.title)
        internal val category: TextView by bindView(R.id.category)
        internal val time: TextView by bindView(R.id.time)

        init {
            itemView.setOnClickListener {
                withSafeAdapterPosition(this) {
                    clickSubject.onNext(data[it])
                }
            }

            image.setOnClickListener { view ->
                withSafeAdapterPosition(this) {
                    imageClickSubject.onNext(view as ImageView to data[it])
                }
            }

            expand.setOnClickListener {
                withSafeAdapterPosition(this) {
                    val id = data[it].id

                    if (expansionMap[id] == true) {
                        expansionMap.remove(id)
                    } else {
                        expansionMap.put(id, true)

                        expansionSubject.onNext(data[it])
                    }

                    notifyItemChanged(it)
                }
            }

            expand.setImageDrawable(IconicsDrawable(expand.context)
                    .color(Color.parseColor("#61000000"))
                    .sizeDp(32)
                    .paddingDp(8)
                    .icon(CommunityMaterial.Icon.cmd_chevron_down))
        }

        fun bind(item: NewsArticle) {
            ViewCompat.setTransitionName(image, "news_${item.id}")

            title.text = item.subject
            description.text = item.description.trim()
            category.text = item.category
            time.text = item.date.convertToRelativeReadableTime(time.context)

            if (expansionMap[item.id] == true) {
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

            glide.defaultLoad(image, ProxerUrls.newsImage(item.id, item.image))
        }
    }
}
