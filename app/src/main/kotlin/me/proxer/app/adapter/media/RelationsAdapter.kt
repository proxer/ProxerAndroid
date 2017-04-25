package me.proxer.app.adapter.media

import android.support.v4.view.ViewCompat
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
import me.proxer.app.util.extension.toGeneralLanguage
import me.proxer.library.entitiy.info.Relation
import me.proxer.library.enums.Category
import me.proxer.library.enums.Language
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
class RelationsAdapter(private val glide: RequestManager) : PagingAdapter<Relation>() {

    var callback: RelationsAdapterCallback? = null

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = internalList[position].id.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagingViewHolder<Relation> {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_media_entry, parent, false))
    }

    override fun destroy() {
        super.destroy()

        callback = null
        glide.onDestroy()
    }

    inner class ViewHolder(itemView: View) : PagingViewHolder<Relation>(itemView) {

        private val title: TextView by bindView(R.id.title)
        private val medium: TextView by bindView(R.id.medium)
        private val image: ImageView by bindView(R.id.image)
        private val ratingContainer: ViewGroup by bindView(R.id.ratingContainer)
        private val rating: RatingBar by bindView(R.id.rating)
        private val episodes: TextView by bindView(R.id.episodes)
        private val english: ImageView by bindView(R.id.english)
        private val german: ImageView by bindView(R.id.german)

        init {
            itemView.setOnClickListener { view ->
                withSafeAdapterPosition {
                    callback?.onRelationClick(view, internalList[adapterPosition])
                }
            }
        }

        override fun bind(item: Relation) {
            ViewCompat.setTransitionName(image, "relation_${item.id}")

            title.text = item.name
            medium.text = item.medium.toAppString(medium.context)
            episodes.text = episodes.resources.getQuantityString(when (item.category) {
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
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(image)
        }
    }

    interface RelationsAdapterCallback {
        fun onRelationClick(view: View, item: Relation) {}
    }
}