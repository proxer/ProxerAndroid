package me.proxer.app.adapter.media

import android.support.v4.view.ViewCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import me.proxer.app.R
import me.proxer.app.adapter.base.BaseAdapter
import me.proxer.app.application.GlideRequests
import me.proxer.app.util.extension.bindView
import me.proxer.app.util.extension.toAppString
import me.proxer.app.util.extension.toGeneralLanguage
import me.proxer.library.entitiy.list.MediaListEntry
import me.proxer.library.enums.Category
import me.proxer.library.enums.Language
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
class MediaAdapter(private val category: Category, private val glide: GlideRequests) : BaseAdapter<MediaListEntry>() {

    var callback: MediaAdapterCallback? = null

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_media_entry, parent, false))
    }

    override fun onViewRecycled(holder: PagingViewHolder<MediaListEntry>) {
        if (holder is ViewHolder) {
            glide.clear(holder.image)
        }
    }

    override fun destroy() {
        super.destroy()

        callback = null
    }

    inner class ViewHolder(itemView: View) : PagingViewHolder<MediaListEntry>(itemView) {

        internal val title: TextView by bindView(R.id.title)
        internal val medium: TextView by bindView(R.id.medium)
        internal val image: ImageView by bindView(R.id.image)
        internal val ratingContainer: ViewGroup by bindView(R.id.ratingContainer)
        internal val rating: RatingBar by bindView(R.id.rating)
        internal val episodes: TextView by bindView(R.id.episodes)
        internal val english: ImageView by bindView(R.id.english)
        internal val german: ImageView by bindView(R.id.german)

        init {
            itemView.setOnClickListener { view ->
                withSafeAdapterPosition {
                    callback?.onMediaClick(view, internalList[it])
                }
            }
        }

        override fun bind(item: MediaListEntry) {
            ViewCompat.setTransitionName(image, "media_${item.id}")

            title.text = item.name
            medium.text = item.medium.toAppString(medium.context)
            episodes.text = episodes.resources.getQuantityString(when (category) {
                Category.ANIME -> R.plurals.media_episode_count
                Category.MANGA -> R.plurals.media_chapter_count
            }, item.episodeAmount, item.episodeAmount)

            val generalLanguages = item.languages.map { it.toGeneralLanguage() }.distinct()

            english.visibility = when (generalLanguages.contains(Language.ENGLISH)) {
                true -> View.VISIBLE
                false -> View.GONE
            }

            german.visibility = when (generalLanguages.contains(Language.GERMAN)) {
                true -> View.VISIBLE
                false -> View.GONE
            }

            if (item.rating > 0) {
                ratingContainer.visibility = View.VISIBLE
                rating.rating = item.rating / 2.0f
            } else {
                ratingContainer.visibility = View.GONE
            }

            glide.load(ProxerUrls.entryImage(item.id).toString())
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(image)
        }
    }

    interface MediaAdapterCallback {
        fun onMediaClick(view: View, item: MediaListEntry) {}
    }
}
