package me.proxer.app.news

import android.os.Bundle
import android.support.v4.view.ViewCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.base.BaseAdapter
import me.proxer.app.util.data.ParcelableStringBooleanMap
import me.proxer.app.util.extension.convertToRelativeReadableTime
import me.proxer.app.util.extension.defaultLoad
import me.proxer.app.util.extension.setIconicsImage
import me.proxer.library.entity.notifications.NewsArticle
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
class NewsAdapter(savedInstanceState: Bundle?) : BaseAdapter<NewsArticle, NewsAdapter.ViewHolder>() {

    private companion object {
        private const val EXPANDED_STATE = "news_expansion_map"
    }

    var glide: GlideRequests? = null
    val clickSubject: PublishSubject<NewsArticle> = PublishSubject.create()
    val expansionSubject: PublishSubject<NewsArticle> = PublishSubject.create()
    val imageClickSubject: PublishSubject<Pair<ImageView, NewsArticle>> = PublishSubject.create()

    private val expansionMap: ParcelableStringBooleanMap

    init {
        expansionMap = when (savedInstanceState) {
            null -> ParcelableStringBooleanMap()
            else -> savedInstanceState.getParcelable(EXPANDED_STATE)
        }

        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_news, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(data[position])

    override fun onViewRecycled(holder: ViewHolder) {
        glide?.clear(holder.image)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView?) {
        glide = null
    }

    override fun areContentsTheSame(old: NewsArticle, new: NewsArticle) = old.date == new.date
            && old.category == new.category
            && old.image == new.image
            && old.subject == new.subject
            && old.description == new.description

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

                    if (expansionMap.containsKey(id)) {
                        expansionMap.remove(id)
                    } else {
                        expansionMap.put(id, true)
                        expansionSubject.onNext(data[it])
                    }

                    notifyItemChanged(it)
                }
            }

            expand.setIconicsImage(CommunityMaterial.Icon.cmd_chevron_down, 32)
        }

        fun bind(item: NewsArticle) {
            ViewCompat.setTransitionName(image, "news_${item.id}")

            title.text = item.subject
            description.text = item.description.trim()
            category.text = item.category
            time.text = item.date.convertToRelativeReadableTime(time.context)

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

            glide?.defaultLoad(image, ProxerUrls.newsImage(item.id, item.image))
        }
    }
}
