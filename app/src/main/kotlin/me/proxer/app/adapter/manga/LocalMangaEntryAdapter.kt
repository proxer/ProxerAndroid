package me.proxer.app.adapter.manga

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import me.proxer.app.R
import me.proxer.app.adapter.base.PagingAdapter
import me.proxer.app.entity.LocalMangaChapter
import me.proxer.app.util.extension.bindView
import me.proxer.app.util.extension.toEpisodeAppString
import me.proxer.library.enums.Category

/**
 * @author Ruben Gees
 */
internal class LocalMangaEntryAdapter : PagingAdapter<LocalMangaChapter>() {

    init {
        setHasStableIds(true)
    }

    var callback: LocalMangaEntryAdapterCallback? = null

    override fun getItemId(position: Int) = internalList[position].episode.toLong()

    override fun areItemsTheSame(oldItem: LocalMangaChapter, newItem: LocalMangaChapter)
            = oldItem.episode == newItem.episode

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
            = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_local_manga_chapter, parent, false))

    override fun destroy() {
        super.destroy()

        callback = null
    }

    inner class ViewHolder(itemView: View) : PagingViewHolder<LocalMangaChapter>(itemView) {

        private val title: TextView by bindView(R.id.title)
        private val delete: ImageView by bindView(R.id.delete)

        init {
            itemView.setOnClickListener {
                withSafeAdapterPosition {
                    callback?.onChapterClick(internalList[it])
                }
            }

            delete.setImageDrawable(IconicsDrawable(delete.context, CommunityMaterial.Icon.cmd_delete)
                    .colorRes(R.color.icon)
                    .sizeDp(16))

            delete.setOnClickListener {
                withSafeAdapterPosition {
                    callback?.onDeleteClick(internalList[it])
                }
            }
        }

        override fun bind(item: LocalMangaChapter) {
            title.text = when {
                item.title.isNotBlank() -> item.title
                else -> Category.MANGA.toEpisodeAppString(title.context, item.episode)
            }
        }
    }

    interface LocalMangaEntryAdapterCallback {
        fun onChapterClick(chapter: LocalMangaChapter) {}
        fun onDeleteClick(chapter: LocalMangaChapter) {}
    }
}