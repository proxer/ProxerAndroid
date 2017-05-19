package me.proxer.app.adapter.manga

import android.os.Bundle
import android.support.v4.view.ViewCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import me.proxer.app.R
import me.proxer.app.adapter.base.BaseAdapter
import me.proxer.app.adapter.manga.LocalMangaChapterAdapter.LocalMangaChapterAdapterCallback
import me.proxer.app.application.GlideRequests
import me.proxer.app.entity.manga.LocalMangaChapter
import me.proxer.app.util.PaddingDividerItemDecoration
import me.proxer.app.util.ParcelableStringBooleanMap
import me.proxer.app.util.extension.CompleteLocalMangaEntry
import me.proxer.app.util.extension.bindView
import me.proxer.library.entitiy.info.EntryCore
import me.proxer.library.util.ProxerUrls


/**
 * @author Ruben Gees
 */
class LocalMangaAdapter(savedInstanceState: Bundle?, private val glide: GlideRequests) :
        BaseAdapter<CompleteLocalMangaEntry>() {

    private companion object {
        private const val EXPANDED_STATE = "local_manga_expanded"
    }

    private val expanded: ParcelableStringBooleanMap

    var callback: LocalMangaAdapterCallback? = null

    init {
        expanded = when (savedInstanceState) {
            null -> ParcelableStringBooleanMap()
            else -> savedInstanceState.getParcelable(EXPANDED_STATE)
        }

        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = internalList[position].first.id.toLong()

    override fun onViewRecycled(holder: BaseViewHolder<CompleteLocalMangaEntry>) {
        if (holder is ViewHolder) {
            glide.clear(holder.image)
        }
    }

    override fun areItemsTheSame(oldItem: CompleteLocalMangaEntry, newItem: CompleteLocalMangaEntry)
            = oldItem.first.id == newItem.first.id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<CompleteLocalMangaEntry>
            = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_local_manga, parent, false))

    override fun onViewAttachedToWindow(holder: BaseViewHolder<CompleteLocalMangaEntry>) {
        super.onViewAttachedToWindow(holder)

        if (holder is ViewHolder) {
            holder.adapter.callback = object : LocalMangaChapterAdapterCallback {
                override fun onChapterClick(chapter: LocalMangaChapter) = holder.withSafeAdapterPosition {
                    this@LocalMangaAdapter.callback?.onChapterClick(internalList[it].first, chapter)
                }

                override fun onDeleteClick(chapter: LocalMangaChapter) = holder.withSafeAdapterPosition {
                    this@LocalMangaAdapter.callback?.onDeleteClick(internalList[it].first, chapter)
                }
            }
        }
    }

    override fun onViewDetachedFromWindow(holder: BaseViewHolder<CompleteLocalMangaEntry>?) {
        super.onViewDetachedFromWindow(holder)

        if (holder is ViewHolder) {
            holder.adapter.destroy()
        }
    }

    override fun destroy() {
        super.destroy()

        callback = null
    }

    fun saveInstanceState(outState: Bundle) {
        outState.putParcelable(EXPANDED_STATE, expanded)
    }

    internal inner class ViewHolder(itemView: View) : BaseViewHolder<CompleteLocalMangaEntry>(itemView) {

        internal val adapter: LocalMangaChapterAdapter
            get() = chapters.adapter as LocalMangaChapterAdapter

        internal val image: ImageView by bindView(R.id.image)
        internal val title: TextView by bindView(R.id.title)
        internal val chapters: RecyclerView by bindView(R.id.chapters)

        init {
            itemView.setOnClickListener {
                withSafeAdapterPosition {
                    val id = internalList[it].first.id

                    if (expanded[id] ?: false) {
                        expanded.remove(id)
                    } else {
                        expanded.put(id, true)
                    }

                    notifyItemChanged(it)
                }
            }

            itemView.setOnLongClickListener { view ->
                withSafeAdapterPosition {
                    callback?.onChapterLongClick(view, internalList[it].first)
                }

                true
            }

            chapters.isNestedScrollingEnabled = false
            chapters.adapter = LocalMangaChapterAdapter()
            chapters.layoutManager = LinearLayoutManager(itemView.context)
            chapters.addItemDecoration(PaddingDividerItemDecoration(chapters.context, 4f))
        }

        override fun bind(item: CompleteLocalMangaEntry) {
            ViewCompat.setTransitionName(image, "local_manga_${item.first.id}")

            title.text = item.first.name

            if (expanded[item.first.id] ?: false) {
                chapters.visibility = View.VISIBLE
                adapter.replace(item.second)
            } else {
                chapters.visibility = View.GONE
                adapter.clear()
            }

            glide.load(ProxerUrls.entryImage(item.first.id).toString())
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(image)
        }
    }

    interface LocalMangaAdapterCallback {
        fun onChapterClick(entry: EntryCore, chapter: LocalMangaChapter) {}
        fun onChapterLongClick(view: View, entry: EntryCore) {}
        fun onDeleteClick(entry: EntryCore, chapter: LocalMangaChapter) {}
    }
}