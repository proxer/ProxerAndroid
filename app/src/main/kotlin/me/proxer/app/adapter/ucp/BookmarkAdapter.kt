package me.proxer.app.adapter.ucp

import android.graphics.drawable.ColorDrawable
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import me.proxer.app.R
import me.proxer.app.adapter.base.BaseAdapter
import me.proxer.app.application.GlideRequests
import me.proxer.app.util.extension.*
import me.proxer.library.entitiy.ucp.Bookmark
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
class BookmarkAdapter(private val glide: GlideRequests) : BaseAdapter<Bookmark>() {

    var callback: BookmarkAdapterCallback? = null

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = internalList[position].id.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<Bookmark> {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_bookmark, parent, false))
    }

    override fun onViewRecycled(holder: BaseViewHolder<Bookmark>) {
        if (holder is ViewHolder) {
            glide.clear(holder.image)
        }
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

    internal inner class ViewHolder(itemView: View) : BaseViewHolder<Bookmark>(itemView) {

        internal val title: TextView by bindView(R.id.title)
        internal val medium: TextView by bindView(R.id.medium)
        internal val image: ImageView by bindView(R.id.image)
        internal val episode: TextView by bindView(R.id.episode)
        internal val availability: ImageView by bindView(R.id.availability)
        internal val language: ImageView by bindView(R.id.language)
        internal val remove: ImageButton by bindView(R.id.remove)

        init {
            itemView.setOnClickListener {
                withSafeAdapterPosition {
                    callback?.onBookmarkClick(internalList[it])
                }
            }

            itemView.setOnLongClickListener { view ->
                withSafeAdapterPosition {
                    callback?.onBookmarkLongClick(view, internalList[it])
                }

                true
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
            ViewCompat.setTransitionName(image, "bookmark_${item.id}")

            title.text = item.name
            medium.text = item.medium.toAppString(medium.context)
            episode.text = item.category.toEpisodeAppString(episode.context, item.episode)
            availability.setImageDrawable(ColorDrawable(ContextCompat.getColor(availability.context,
                    when (item.isAvailable) {
                        true -> R.color.md_green_500
                        false -> R.color.md_red_500
                    })))

            language.setImageDrawable(item.language.toGeneralLanguage().toAppDrawable(language.context))

            glide.load(ProxerUrls.entryImage(item.entryId).toString())
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(image)
        }
    }

    interface BookmarkAdapterCallback {
        fun onBookmarkClick(item: Bookmark) {}
        fun onBookmarkLongClick(view: View, item: Bookmark) {}
        fun onBookmarkRemoval(item: Bookmark) {}
    }
}
