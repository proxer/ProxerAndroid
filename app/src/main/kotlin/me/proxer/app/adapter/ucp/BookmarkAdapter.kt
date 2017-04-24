package me.proxer.app.adapter.ucp

import android.graphics.drawable.ColorDrawable
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import me.proxer.app.R
import me.proxer.app.adapter.base.PagingAdapter
import me.proxer.app.util.extension.bindView
import me.proxer.app.util.extension.toAppString
import me.proxer.app.util.extension.toGeneralLanguage
import me.proxer.library.entitiy.ucp.Bookmark
import me.proxer.library.enums.Language
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
class BookmarkAdapter(private val glide: RequestManager) : PagingAdapter<Bookmark>() {

    var callback: BookmarkAdapterCallback? = null

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = internalList[position].id.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagingViewHolder<Bookmark> {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_bookmark, parent, false))
    }

    override fun destroy() {
        super.destroy()

        callback = null
    }

    override fun areItemsTheSame(oldItem: Bookmark, newItem: Bookmark): Boolean {
        return oldItem.entryId == newItem.entryId
    }

    override fun areContentsTheSame(oldItem: Bookmark, newItem: Bookmark): Boolean {
        return oldItem.id == newItem.id
    }

    inner class ViewHolder(itemView: View) : PagingViewHolder<Bookmark>(itemView) {

        private val title: TextView by bindView(R.id.title)
        private val medium: TextView by bindView(R.id.medium)
        private val image: ImageView by bindView(R.id.image)
        private val episode: TextView by bindView(R.id.episode)
        private val availability: ImageView by bindView(R.id.availability)
        private val language: ImageView by bindView(R.id.language)
        private val remove: ImageButton by bindView(R.id.remove)

        init {
            itemView.setOnClickListener {
                withSafeAdapterPosition {
                    callback?.onBookmarkClick(internalList[it])
                }
            }

            remove.setImageDrawable(IconicsDrawable(remove.context)
                    .icon(CommunityMaterial.Icon.cmd_bookmark_remove)
                    .colorRes(R.color.icon)
                    .sizeDp(48)
                    .paddingDp(12))

            remove.setOnClickListener {
                withSafeAdapterPosition {
                    callback?.onBookmarkRemoval(internalList[it])
                }
            }
        }

        override fun bind(item: Bookmark) {
            title.text = item.name
            medium.text = item.medium.toAppString(medium.context)
            episode.text = episode.context.getString(R.string.bookmark_episode, item.episode)
            availability.setImageDrawable(ColorDrawable(ContextCompat.getColor(availability.context,
                    when (item.isAvailable) {
                        true -> R.color.md_green_500
                        false -> R.color.md_red_500
                    })))

            language.setImageResource(when (item.language.toGeneralLanguage()) {
                Language.GERMAN -> R.drawable.ic_germany
                Language.ENGLISH -> R.drawable.ic_united_states
            })

            glide.load(ProxerUrls.entryImage(item.entryId).toString())
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(image)
        }
    }

    interface BookmarkAdapterCallback {
        fun onBookmarkClick(item: Bookmark) {}
        fun onBookmarkRemoval(item: Bookmark) {}
    }
}
