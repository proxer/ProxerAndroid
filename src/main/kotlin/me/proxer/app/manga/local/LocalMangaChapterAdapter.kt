package me.proxer.app.manga.local

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseAdapter
import me.proxer.app.manga.local.LocalMangaChapterAdapter.ViewHolder
import me.proxer.app.util.extension.toEpisodeAppString
import me.proxer.library.enums.Category

/**
 * @author Ruben Gees
 */
internal class LocalMangaChapterAdapter : BaseAdapter<LocalMangaChapter, ViewHolder>() {

    var callback: LocalMangaChapterAdapterCallback? = null

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_local_manga_chapter, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(data[position])

    override fun getItemId(position: Int) = data[position].episode.toLong()
    override fun areItemsTheSame(old: LocalMangaChapter, new: LocalMangaChapter) = old.episode == new.episode

    internal inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal val title: TextView by bindView(R.id.title)
        internal val delete: ImageView by bindView(R.id.delete)

        init {
            itemView.setOnClickListener {
                withSafeAdapterPosition(this) {
                    callback?.onChapterClick(data[it])
                }
            }

            delete.setOnClickListener {
                withSafeAdapterPosition(this) {
                    callback?.onDeleteClick(data[it])
                }
            }

            delete.setImageDrawable(IconicsDrawable(delete.context, CommunityMaterial.Icon.cmd_delete)
                    .colorRes(R.color.icon)
                    .sizeDp(16))
        }

        fun bind(item: LocalMangaChapter) {
            title.text = when {
                item.title.isNotBlank() -> item.title.trim()
                else -> Category.MANGA.toEpisodeAppString(title.context, item.episode)
            }
        }
    }

    internal interface LocalMangaChapterAdapterCallback {
        fun onChapterClick(chapter: LocalMangaChapter) = Unit
        fun onDeleteClick(chapter: LocalMangaChapter) = Unit
    }
}
