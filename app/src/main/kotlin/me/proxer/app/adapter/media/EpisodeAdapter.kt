package me.proxer.app.adapter.media

import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import me.proxer.app.R
import me.proxer.app.adapter.base.PagingAdapter
import me.proxer.app.application.MainApplication.Companion.mangaDb
import me.proxer.app.entity.EpisodeRow
import me.proxer.app.event.LocalMangaJobFailedEvent
import me.proxer.app.event.LocalMangaJobFinishedEvent
import me.proxer.app.event.LoginEvent
import me.proxer.app.event.LogoutEvent
import me.proxer.app.helper.StorageHelper
import me.proxer.app.job.LocalMangaJob
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.ParcelableStringBooleanMap
import me.proxer.app.util.extension.*
import me.proxer.library.enums.Category
import me.proxer.library.enums.MediaLanguage
import me.proxer.library.util.ProxerUrls
import me.zhanghai.android.materialprogressbar.MaterialProgressBar
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.collections.forEachWithIndex
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.find
import org.jetbrains.anko.forEachChildWithIndex
import org.jetbrains.anko.uiThread
import java.util.concurrent.Future

/**
 * @author Ruben Gees
 */
class EpisodeAdapter(private val entryId: String, savedInstanceState: Bundle?) : PagingAdapter<EpisodeRow>() {

    private companion object {
        private const val EXPANDED_STATE = "episode_expanded"
    }

    private val expanded: ParcelableStringBooleanMap
    private var isLoggedIn = StorageHelper.user != null

    var callback: EpisodeAdapterCallback? = null

    init {
        expanded = when (savedInstanceState) {
            null -> ParcelableStringBooleanMap()
            else -> savedInstanceState.getParcelable(EXPANDED_STATE)
        }

        setHasStableIds(true)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView?) {
        super.onAttachedToRecyclerView(recyclerView)

        EventBus.getDefault().register(this)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView?) {
        super.onDetachedFromRecyclerView(recyclerView)

        EventBus.getDefault().unregister(this)
    }

    override fun onViewAttachedToWindow(holder: PagingViewHolder<EpisodeRow>) {
        EventBus.getDefault().register(holder)
    }

    override fun onViewDetachedFromWindow(holder: PagingViewHolder<EpisodeRow>) {
        EventBus.getDefault().unregister(holder)
    }

    override fun getItemId(position: Int): Long {
        return internalList[position].number.toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagingViewHolder<EpisodeRow> {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_episode, parent, false))
    }

    override fun areItemsTheSame(oldItem: EpisodeRow, newItem: EpisodeRow) = oldItem.number == newItem.number

    override fun destroy() {
        super.destroy()

        callback = null
    }

    fun saveInstanceState(outState: Bundle) {
        outState.putParcelable(EXPANDED_STATE, expanded)
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLogin(@Suppress("UNUSED_PARAMETER") event: LoginEvent) {
        isLoggedIn = true

        notifyDataSetChanged()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLogout(@Suppress("UNUSED_PARAMETER") event: LogoutEvent) {
        isLoggedIn = false

        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : PagingViewHolder<EpisodeRow>(itemView) {

        private val title: TextView by bindView(R.id.title)
        private val titleContainer: ViewGroup by bindView(R.id.titleContainer)
        private val watched: ImageView by bindView(R.id.watched)
        private val languages: ViewGroup by bindView(R.id.languages)

        init {
            titleContainer.setOnClickListener {
                withSafeAdapterPosition {
                    val number = internalList[it].number.toString()

                    if (expanded[number] ?: false) {
                        expanded.remove(number)
                    } else {
                        expanded.put(number, true)
                    }

                    notifyItemChanged(it)
                }
            }

            watched.setImageDrawable(IconicsDrawable(watched.context)
                    .icon(CommunityMaterial.Icon.cmd_check)
                    .sizeDp(24)
                    .colorRes(R.color.icon))
        }

        override fun bind(item: EpisodeRow) {
            title.text = item.title ?: item.category.toEpisodeAppString(title.context, item.number)

            if (item.userProgress >= item.number) {
                watched.visibility = View.VISIBLE
            } else {
                watched.visibility = View.INVISIBLE
            }

            if (expanded[item.number.toString()] ?: false) {
                languages.visibility = View.VISIBLE
            } else {
                languages.visibility = View.GONE

                return
            }

            if (languages.childCount != item.languageHosterList.size) {
                languages.removeAllViews()

                for (i in 0 until item.languageHosterList.size) {
                    View.inflate(languages.context, R.layout.layout_episode_language, languages)
                }
            }

            item.languageHosterList.forEachWithIndex { index, (language, hosterImages) ->
                val languageContainer = languages.getChildAt(index)
                val languageView = languageContainer.find<TextView>(R.id.language)
                val hostersView = languageContainer.find<ViewGroup>(R.id.hosters)
                val download = languageContainer.find<ImageView>(R.id.download)
                val downloadProgress = languageContainer.find<MaterialProgressBar>(R.id.downloadProgress)

                languageView.text = language.toAppString(languageView.context)
                languageView.setCompoundDrawablesWithIntrinsicBounds(language.toGeneralLanguage()
                        .toAppDrawable(languageView.context), null, null, null)

                languageContainer.setOnClickListener {
                    withSafeAdapterPosition {
                        callback?.onLanguageClick(language, internalList[it])
                    }
                }

                bindDownload(item.category, item.number, language, download, downloadProgress)
                bindHosterImages(hosterImages, hostersView)
            }
        }

        @Suppress("unused")
        @Subscribe(threadMode = ThreadMode.MAIN)
        fun onLocalMangaJobFinished(event: LocalMangaJobFinishedEvent) {
            withSafeAdapterPosition {
                if (event.entryId == entryId && event.episode == internalList[it].number) {
                    notifyItemChanged(it)
                }
            }
        }

        @Suppress("unused")
        @Subscribe(threadMode = ThreadMode.MAIN)
        fun onLocalMangaJobFailed(event: LocalMangaJobFailedEvent) {
            withSafeAdapterPosition {
                if (event.id == entryId && event.episode == internalList[it].number) {
                    notifyItemChanged(it)
                }
            }
        }

        private fun bindDownload(category: Category, episode: Int, language: MediaLanguage, download: ImageView,
                                 downloadProgress: MaterialProgressBar) {
            download.tag.let {
                if (it is Future<*>) {
                    it.cancel(true)
                }
            }

            if (category == Category.MANGA && isLoggedIn) {
                download.tag = doAsync {
                    if (mangaDb.containsChapter(entryId, episode, language.toGeneralLanguage())) {
                        val icon = IconicsDrawable(download.context, CommunityMaterial.Icon.cmd_cloud_check)
                                .colorRes(R.color.icon)
                                .sizeDp(32)

                        uiThread {
                            downloadProgress.visibility = View.GONE
                            download.visibility = View.VISIBLE
                            download.setImageDrawable(icon)
                            download.setOnClickListener(null)
                        }
                    } else {
                        if (LocalMangaJob.isScheduledOrRunning(entryId, episode, language.toGeneralLanguage())) {
                            uiThread {
                                download.visibility = View.GONE
                                downloadProgress.visibility = View.VISIBLE
                                downloadProgress.setOnClickListener {
                                    LocalMangaJob.cancel(entryId, episode, language.toGeneralLanguage())

                                    bindDownload(category, episode, language, download, downloadProgress)
                                }
                            }
                        } else {
                            val icon = IconicsDrawable(download.context, CommunityMaterial.Icon.cmd_download)
                                    .colorRes(R.color.icon)
                                    .sizeDp(32)

                            uiThread {
                                downloadProgress.visibility = View.GONE
                                download.visibility = View.VISIBLE
                                download.setImageDrawable(icon)
                                download.setOnClickListener {
                                    LocalMangaJob.schedule(entryId, episode, language.toGeneralLanguage())

                                    bindDownload(category, episode, language, download, downloadProgress)
                                }
                            }
                        }
                    }
                }
            }
        }

        private fun bindHosterImages(hosterImages: List<String>?, hostersView: ViewGroup) {
            if (hosterImages == null || hosterImages.isEmpty()) {
                hostersView.removeAllViews()
                hostersView.visibility = View.GONE
            } else {
                hostersView.visibility = View.VISIBLE

                if (hostersView.childCount != hosterImages.size) {
                    hostersView.removeAllViews()

                    for (i in 0 until hosterImages.size) {
                        val imageView = LayoutInflater.from(hostersView.context)
                                .inflate(R.layout.layout_image, hostersView, false).apply {
                            layoutParams.width = DeviceUtils.convertDpToPx(hostersView.context, 28f)
                            layoutParams.height = DeviceUtils.convertDpToPx(hostersView.context, 28f)
                        }

                        hostersView.addView(imageView)
                    }
                }

                hostersView.forEachChildWithIndex { index, imageView ->
                    Glide.with(imageView.context)
                            .load(ProxerUrls.hosterImage(hosterImages[index]).toString())
                            .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                            .into(imageView as ImageView)
                }
            }
        }
    }

    interface EpisodeAdapterCallback {
        fun onLanguageClick(language: MediaLanguage, episode: EpisodeRow) {}
    }
}