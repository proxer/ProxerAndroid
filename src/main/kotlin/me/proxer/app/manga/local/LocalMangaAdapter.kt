package me.proxer.app.manga.local

import android.os.Bundle
import android.support.v4.view.ViewCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.base.BaseAdapter
import me.proxer.app.manga.local.LocalMangaAdapter.ViewHolder
import me.proxer.app.manga.local.LocalMangaChapterAdapter.LocalMangaChapterAdapterCallback
import me.proxer.app.ui.PaddingDividerItemDecoration
import me.proxer.app.util.data.ParcelableStringBooleanMap
import me.proxer.app.util.extension.CompleteLocalMangaEntry
import me.proxer.app.util.extension.defaultLoad
import me.proxer.library.entity.info.EntryCore
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
class LocalMangaAdapter(savedInstanceState: Bundle?) : BaseAdapter<CompleteLocalMangaEntry, ViewHolder>() {

    private companion object {
        private const val EXPANDED_STATE = "local_manga_expanded"
    }

    var glide: GlideRequests? = null
    val clickSubject: PublishSubject<Pair<EntryCore, LocalMangaChapter>> = PublishSubject.create()
    val longClickSubject: PublishSubject<Pair<ImageView, EntryCore>> = PublishSubject.create()
    val deleteClickSubject: PublishSubject<Pair<EntryCore, LocalMangaChapter>> = PublishSubject.create()

    private val expansionMap: ParcelableStringBooleanMap

    init {
        expansionMap = when (savedInstanceState) {
            null -> ParcelableStringBooleanMap()
            else -> savedInstanceState.getParcelable(EXPANDED_STATE)
        }

        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_local_manga, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(data[position])
    override fun getItemId(position: Int) = data[position].first.id.toLong()

    override fun onViewRecycled(holder: ViewHolder) {
        glide?.clear(holder.image)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView?) {
        glide = null
    }

    override fun areItemsTheSame(old: CompleteLocalMangaEntry, new: CompleteLocalMangaEntry): Boolean {
        return old.first.id == new.first.id
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)

        holder.adapter.callback = object : LocalMangaChapterAdapterCallback {
            override fun onChapterClick(chapter: LocalMangaChapter) = withSafeAdapterPosition(holder) {
                clickSubject.onNext(data[it].first to chapter)
            }

            override fun onDeleteClick(chapter: LocalMangaChapter) = withSafeAdapterPosition(holder) {
                deleteClickSubject.onNext(data[it].first to chapter)
            }
        }
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        super.onViewDetachedFromWindow(holder)

        holder.adapter.callback = null
    }

    override fun saveInstanceState(outState: Bundle) = outState.putParcelable(EXPANDED_STATE, expansionMap)

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal val adapter: LocalMangaChapterAdapter
            get() = chapters.adapter as LocalMangaChapterAdapter

        internal val image: ImageView by bindView(R.id.image)
        internal val title: TextView by bindView(R.id.title)
        internal val chapters: RecyclerView by bindView(R.id.chapters)

        init {
            itemView.setOnClickListener {
                withSafeAdapterPosition(this) {
                    expansionMap.putOrRemove(data[it].first.id)

                    notifyItemChanged(it)
                }
            }

            itemView.setOnLongClickListener {
                withSafeAdapterPosition(this) {
                    longClickSubject.onNext(image to data[it].first)
                }

                true
            }

            chapters.isNestedScrollingEnabled = false
            chapters.layoutManager = LinearLayoutManager(itemView.context)
            chapters.adapter = LocalMangaChapterAdapter()
            chapters.addItemDecoration(PaddingDividerItemDecoration(chapters.context, 4f))
        }

        fun bind(item: CompleteLocalMangaEntry) {
            ViewCompat.setTransitionName(image, "local_manga_${item.first.id}")

            title.text = item.first.name

            if (expansionMap.containsKey(item.first.id)) {
                chapters.visibility = View.VISIBLE

                adapter.swapDataAndNotifyWithDiffing(item.second)
            } else {
                chapters.visibility = View.GONE

                adapter.swapDataAndNotifyWithDiffing(emptyList())
            }

            glide?.defaultLoad(image, ProxerUrls.entryImage(item.first.id))
        }
    }
}
