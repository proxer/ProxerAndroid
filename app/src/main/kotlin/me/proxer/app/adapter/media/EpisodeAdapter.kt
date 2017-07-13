package me.proxer.app.adapter.media

import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import me.proxer.app.R
import me.proxer.app.adapter.base.BaseGlideAdapter
import me.proxer.app.application.GlideRequests
import me.proxer.app.application.MainApplication.Companion.mangaDb
import me.proxer.app.entity.EpisodeRow
import me.proxer.app.event.LoginEvent
import me.proxer.app.event.LogoutEvent
import me.proxer.app.event.manga.LocalMangaJobFailedEvent
import me.proxer.app.event.manga.LocalMangaJobFinishedEvent
import me.proxer.app.helper.StorageHelper
import me.proxer.app.job.LocalMangaJob
import me.proxer.app.util.data.ParcelableStringBooleanMap
import me.proxer.app.util.extension.*
import me.proxer.library.enums.Category
import me.proxer.library.enums.MediaLanguage
import me.proxer.library.util.ProxerUrls
import me.zhanghai.android.materialprogressbar.MaterialProgressBar
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.*
import org.jetbrains.anko.collections.forEachWithIndex
import java.util.concurrent.Future

/**
 * @author Ruben Gees
 */
class EpisodeAdapter(private val entryId: String, savedInstanceState: Bundle?, glide: GlideRequests) :
        BaseGlideAdapter<EpisodeRow>(glide) {

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<EpisodeRow> {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_episode, parent, false))
    }

    override fun getItemId(position: Int): Long {
        return internalList[position].number.toLong()
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView?) {
        super.onAttachedToRecyclerView(recyclerView)

        EventBus.getDefault().register(this)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView?) {
        super.onDetachedFromRecyclerView(recyclerView)

        EventBus.getDefault().unregister(this)
    }

    override fun onViewAttachedToWindow(holder: BaseViewHolder<EpisodeRow>?) {
        super.onViewAttachedToWindow(holder)

        EventBus.getDefault().register(holder)
    }

    override fun onViewDetachedFromWindow(holder: BaseViewHolder<EpisodeRow>?) {
        super.onViewDetachedFromWindow(holder)

        EventBus.getDefault().unregister(holder)
    }

    override fun onViewRecycled(holder: BaseViewHolder<EpisodeRow>?) {
        (holder as? ViewHolder)?.languages?.applyRecursively {
            if (it is ImageView) {
                clearImage(it)
            }
        }
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

    internal inner class ViewHolder(itemView: View) : BaseViewHolder<EpisodeRow>(itemView) {

        internal val title: TextView by bindView(R.id.title)
        internal val titleContainer: ViewGroup by bindView(R.id.titleContainer)
        internal val watched: ImageView by bindView(R.id.watched)
        internal val languages: ViewGroup by bindView(R.id.languages)

        init {
            titleContainer.setOnClickListener {
                withSafeAdapterPosition {
                    val number = internalList[it].number.toString()

                    if (expanded[number] == true) {
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

            if (expanded[item.number.toString()] == true) {
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
                val downloadContainer = languageContainer.find<ViewGroup>(R.id.downloadContainer)
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

                bindHosterImages(hosterImages, hostersView)
                bindDownload(item.category, item.number, language, downloadContainer, download, downloadProgress)
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

        private fun bindHosterImages(hosterImages: List<String>?, hostersView: ViewGroup) {
            if (hosterImages == null || hosterImages.isEmpty()) {
                hostersView.removeAllViews()

                hostersView.visibility = View.GONE
            } else {
                hostersView.visibility = View.VISIBLE

                if (hostersView.childCount != hosterImages.size) {
                    hostersView.removeAllViews()

                    for (i in 0 until hosterImages.size) {
                        val inflater = LayoutInflater.from(hostersView.context)
                        val imageView = inflater.inflate(R.layout.layout_image, hostersView, false).apply {
                            layoutParams.width = dip(28)
                            layoutParams.height = dip(28)
                        }

                        hostersView.addView(imageView)
                    }
                }

                hostersView.forEachChildWithIndex { index, imageView ->
                    loadImage(imageView as ImageView, ProxerUrls.hosterImage(hosterImages[index]))
                }
            }
        }

        private fun bindDownload(category: Category, episode: Int, language: MediaLanguage,
                                 downloadContainer: ViewGroup, download: ImageView,
                                 downloadProgress: MaterialProgressBar) {

            downloadProgress.tag.let {
                (it as? Future<*>)?.cancel(true)
            }

            if (category == Category.MANGA && isLoggedIn) {
                download.setOnClickListener {
                    doAsync {
                        if (mangaDb.containsChapter(entryId, episode, language.toGeneralLanguage())) {
                            uiThread {
                                download.toastBelow(R.string.fragment_episode_already_downloaded_hint)
                            }
                        } else {
                            LocalMangaJob.schedule(it.context, entryId, episode, language.toGeneralLanguage())

                            uiThread {
                                download.visibility = View.INVISIBLE
                                downloadProgress.visibility = View.VISIBLE
                            }
                        }
                    }
                }

                download.setOnLongClickListener {
                    doAsync {
                        if (mangaDb.containsChapter(entryId, episode, language.toGeneralLanguage())) {
                            uiThread {
                                download.toastBelow(R.string.fragment_episode_already_downloaded_hint)
                            }
                        } else {
                            uiThread {
                                download.toastBelow(R.string.fragment_episode_download_hint)
                            }
                        }
                    }

                    true
                }

                downloadProgress.setOnClickListener { _: View ->
                    doAsync {
                        LocalMangaJob.cancel(entryId, episode, language.toGeneralLanguage())

                        uiThread {
                            download.visibility = View.VISIBLE
                            downloadProgress.visibility = View.INVISIBLE
                        }
                    }
                }

                downloadProgress.tag = doAsync {
                    val containsChapter = mangaDb.containsChapter(entryId, episode, language.toGeneralLanguage())
                    val isScheduledOrRunning = LocalMangaJob.isScheduledOrRunning(entryId, episode,
                            language.toGeneralLanguage())

                    val progressVisibility = when (isScheduledOrRunning) {
                        true -> View.VISIBLE
                        false -> View.INVISIBLE
                    }

                    val downloadVisibility = when (isScheduledOrRunning) {
                        true -> View.INVISIBLE
                        false -> View.VISIBLE
                    }

                    val icon = IconicsDrawable(download.context, when (containsChapter) {
                        true -> CommunityMaterial.Icon.cmd_cloud_check
                        false -> CommunityMaterial.Icon.cmd_download
                    }).colorRes(R.color.icon).sizeDp(32)

                    uiThread {
                        downloadContainer.visibility = View.VISIBLE
                        downloadProgress.visibility = progressVisibility
                        download.visibility = downloadVisibility
                        download.setImageDrawable(icon)
                    }
                }
            } else {
                downloadContainer.visibility = View.GONE
            }
        }
    }

    interface EpisodeAdapterCallback {
        fun onLanguageClick(language: MediaLanguage, episode: EpisodeRow) {}
    }
}