package me.proxer.app.adapter.manga

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import me.proxer.app.R
import me.proxer.app.adapter.base.BaseAdapter
import me.proxer.app.entity.manga.LocalMangaChapter
import me.proxer.app.util.extension.bindView
import me.proxer.app.util.extension.toEpisodeAppString
import me.proxer.app.util.extension.toastBelow
import me.proxer.library.enums.Category

/**
 * @author Ruben Gees
 */
internal class LocalMangaChapterAdapter : BaseAdapter<LocalMangaChapter>() {

    var callback: LocalMangaChapterAdapterCallback? = null

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = internalList[position].episode.toLong()

    override fun areItemsTheSame(oldItem: LocalMangaChapter, newItem: LocalMangaChapter)
            = oldItem.episode == newItem.episode

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
            = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_local_manga_chapter, parent, false))

    override fun destroy() {
        super.destroy()

        callback = null
    }

    internal inner class ViewHolder(itemView: View) : BaseViewHolder<LocalMangaChapter>(itemView) {

        internal val title: TextView by bindView(R.id.title)
        internal val delete: ImageView by bindView(R.id.delete)

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

            delete.setOnLongClickListener {
                it.toastBelow(R.string.fragment_local_manga_delete_hint)

                true
            }
        }

        override fun bind(item: LocalMangaChapter) {
            title.text = when {
                item.title.isNotBlank() -> item.title
                else -> Category.MANGA.toEpisodeAppString(title.context, item.episode)
            }
        }
    }

    interface LocalMangaChapterAdapterCallback {
        fun onChapterClick(chapter: LocalMangaChapter) {}
        fun onDeleteClick(chapter: LocalMangaChapter) {}
    }
}