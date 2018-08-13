package me.proxer.app.media.list

import android.support.v4.view.ViewCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.RelativeLayout
import android.widget.TextView
import com.jakewharton.rxbinding2.view.clicks
import com.uber.autodispose.kotlin.autoDisposable
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.base.AutoDisposeViewHolder
import me.proxer.app.base.BaseAdapter
import me.proxer.app.util.extension.defaultLoad
import me.proxer.app.util.extension.getQuantityString
import me.proxer.app.util.extension.mapAdapterPosition
import me.proxer.app.util.extension.toAppDrawable
import me.proxer.app.util.extension.toAppString
import me.proxer.app.util.extension.toGeneralLanguage
import me.proxer.library.entity.list.MediaListEntry
import me.proxer.library.enums.Category
import me.proxer.library.enums.Language
import me.proxer.library.util.ProxerUrls
import org.jetbrains.anko.below

/**
 * @author Ruben Gees
 */
class MediaAdapter(private val category: Category) : BaseAdapter<MediaListEntry, MediaAdapter.ViewHolder>() {

    var glide: GlideRequests? = null
    val clickSubject: PublishSubject<Pair<ImageView, MediaListEntry>> = PublishSubject.create()

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(LayoutInflater.from(parent.context)
        .inflate(R.layout.item_media_entry, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(data[position])

    override fun onViewRecycled(holder: ViewHolder) {
        glide?.clear(holder.image)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        glide = null
    }

    override fun swapDataAndNotifyWithDiffing(newData: List<MediaListEntry>) {
        super.swapDataAndNotifyWithDiffing(newData.distinctBy { it.id })
    }

    inner class ViewHolder(itemView: View) : AutoDisposeViewHolder(itemView) {

        internal val title: TextView by bindView(R.id.title)
        internal val medium: TextView by bindView(R.id.medium)
        internal val image: ImageView by bindView(R.id.image)
        internal val ratingContainer: ViewGroup by bindView(R.id.ratingContainer)
        internal val rating: RatingBar by bindView(R.id.rating)
        internal val state: ImageView by bindView(R.id.state)
        internal val episodes: TextView by bindView(R.id.episodes)
        internal val english: ImageView by bindView(R.id.english)
        internal val german: ImageView by bindView(R.id.german)

        fun bind(item: MediaListEntry) {
            itemView.clicks()
                .mapAdapterPosition({ adapterPosition }) { image to data[it] }
                .autoDisposable(this)
                .subscribe(clickSubject)

            ViewCompat.setTransitionName(image, "media_${item.id}")

            title.text = item.name
            medium.text = item.medium.toAppString(medium.context)
            episodes.text = episodes.context.getQuantityString(when (category) {
                Category.ANIME -> R.plurals.media_episode_count
                Category.MANGA -> R.plurals.media_chapter_count
            }, item.episodeAmount)

            item.languages.map { it.toGeneralLanguage() }.distinct().let { generalLanguages ->
                english.visibility = when (generalLanguages.contains(Language.ENGLISH)) {
                    true -> View.VISIBLE
                    false -> View.GONE
                }

                german.visibility = when (generalLanguages.contains(Language.GERMAN)) {
                    true -> View.VISIBLE
                    false -> View.GONE
                }
            }

            if (item.rating > 0) {
                ratingContainer.visibility = View.VISIBLE
                rating.rating = item.rating / 2.0f

                (episodes.layoutParams as RelativeLayout.LayoutParams).apply {
                    addRule(RelativeLayout.ALIGN_BOTTOM, 0)
                    below(R.id.state)
                }
            } else {
                ratingContainer.visibility = View.GONE

                (episodes.layoutParams as RelativeLayout.LayoutParams).apply {
                    addRule(RelativeLayout.ALIGN_BOTTOM, R.id.languageContainer)
                    below(R.id.medium)
                }
            }

            state.setImageDrawable(item.state.toAppDrawable(state.context))
            glide?.defaultLoad(image, ProxerUrls.entryImage(item.id))
        }
    }
}
