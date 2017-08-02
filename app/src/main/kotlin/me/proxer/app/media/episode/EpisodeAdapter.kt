package me.proxer.app.media.episode

import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.TooltipCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.GlideRequests
import me.proxer.app.MainApplication.Companion.bus
import me.proxer.app.MainApplication.Companion.mangaDao
import me.proxer.app.R
import me.proxer.app.auth.LoginEvent
import me.proxer.app.auth.LogoutEvent
import me.proxer.app.base.BaseAdapter
import me.proxer.app.manga.local.LocalMangaJob
import me.proxer.app.media.episode.EpisodeAdapter.ViewHolder
import me.proxer.app.util.data.ParcelableStringBooleanMap
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.*
import me.proxer.library.enums.Category
import me.proxer.library.enums.Language
import me.proxer.library.enums.MediaLanguage
import me.proxer.library.util.ProxerUrls
import me.zhanghai.android.materialprogressbar.MaterialProgressBar
import org.jetbrains.anko.applyRecursively
import org.jetbrains.anko.collections.forEachWithIndex
import org.jetbrains.anko.dip
import org.jetbrains.anko.find
import org.jetbrains.anko.forEachChildWithIndex

/**
 * @author Ruben Gees
 */
class EpisodeAdapter(savedInstanceState: Bundle?, private val entryId: String, private val glide: GlideRequests)
    : BaseAdapter<EpisodeRow, ViewHolder>() {

    private companion object {
        private const val EXPANDED_STATE = "episode_expanded"
    }

    val languageClickSubject: PublishSubject<Pair<MediaLanguage, EpisodeRow>> = PublishSubject.create()

    private val expanded: ParcelableStringBooleanMap
    private var isLoggedIn = StorageHelper.user != null

    private var busDisposable: Disposable? = null

    init {
        expanded = when (savedInstanceState) {
            null -> ParcelableStringBooleanMap()
            else -> savedInstanceState.getParcelable(EXPANDED_STATE)
        }

        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_episode, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(data[position])
    override fun getItemId(position: Int) = data[position].number.toLong()

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView?) {
        busDisposable = Observable.merge(bus.register(LoginEvent::class.java), bus.register(LogoutEvent::class.java))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    isLoggedIn = when (it) {
                        is LoginEvent -> true
                        is LogoutEvent -> false
                        else -> false
                    }

                    notifyDataSetChanged()
                }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView?) {
        busDisposable?.dispose()
        busDisposable = null
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) = holder.registerBus()
    override fun onViewDetachedFromWindow(holder: ViewHolder) = holder.disposeBus()

    override fun onViewRecycled(holder: ViewHolder) {
        holder.languages.applyRecursively {
            if (it is ImageView) {
                glide.clear(it)
            }
        }
    }

    override fun areItemsTheSame(old: EpisodeRow, new: EpisodeRow) = old.number == new.number
    override fun saveInstanceState(outState: Bundle) = outState.putParcelable(EXPANDED_STATE, expanded)

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal val title: TextView by bindView(R.id.title)
        internal val titleContainer: ViewGroup by bindView(R.id.titleContainer)
        internal val watched: ImageView by bindView(R.id.watched)
        internal val languages: ViewGroup by bindView(R.id.languages)

        private var jobDisposable: Disposable? = null

        init {
            titleContainer.setOnClickListener {
                withSafeAdapterPosition(this) {
                    val number = data[it].number.toString()

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

        fun bind(item: EpisodeRow) {
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
                    withSafeAdapterPosition(this) {
                        languageClickSubject.onNext(language to data[it])
                    }
                }

                bindHosterImages(hosterImages, hostersView)
                bindDownload(item.category, item.number, language, downloadContainer, download, downloadProgress)
            }
        }

        fun registerBus() {
            jobDisposable = Observable.merge(
                    bus.register(LocalMangaJob.FinishedEvent::class.java),
                    bus.register(LocalMangaJob.FailedEvent::class.java))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { event ->
                        withSafeAdapterPosition(this) {
                            val (entryId, episode) = when (event) {
                                is LocalMangaJob.FinishedEvent -> event.entryId to event.episode
                                is LocalMangaJob.FailedEvent -> event.entryId to event.episode
                                else -> throw IllegalArgumentException("Unknown event: $event")

                            }
                            if (entryId == entryId && episode == data[it].number) {
                                notifyItemChanged(it)
                            }
                        }
                    }
        }

        fun disposeBus() {
            jobDisposable?.dispose()
            jobDisposable = null
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
                    glide.defaultLoad(imageView as ImageView, ProxerUrls.hosterImage(hosterImages[index]))
                }
            }
        }

        private fun bindDownload(category: Category, episode: Int, language: MediaLanguage,
                                 downloadContainer: ViewGroup, download: ImageView,
                                 downloadProgress: MaterialProgressBar) {

            downloadProgress.tag.let { (it as? Disposable)?.dispose() }
            downloadProgress.tag = null

            if (category == Category.MANGA && isLoggedIn) {
                language.toGeneralLanguage().let { generalLanguage ->
                    download.setOnClickListener {
                        constructChapterCheckSingle(entryId, episode, generalLanguage)
                                .doOnSuccess { exists ->
                                    if (!exists) {
                                        LocalMangaJob.schedule(download.context, entryId, episode, generalLanguage)
                                    }
                                }
                                .subscribeOn(Schedulers.newThread())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe { exists: Boolean ->
                                    if (!exists) {
                                        download.visibility = View.INVISIBLE
                                        downloadProgress.visibility = View.VISIBLE
                                    }
                                }
                    }

                    downloadProgress.setOnClickListener {
                        Single.fromCallable { LocalMangaJob.cancel(entryId, episode, generalLanguage) }
                                .subscribeOn(Schedulers.newThread())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe { _: Unit ->
                                    download.visibility = View.VISIBLE
                                    downloadProgress.visibility = View.INVISIBLE
                                }
                    }

                    downloadProgress.tag = constructChapterCheckSingle(entryId, episode, generalLanguage)
                            .map { it to LocalMangaJob.isScheduledOrRunning(entryId, episode, generalLanguage) }
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe { (containsChapter, isScheduledOrRunning) ->
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

                                when (containsChapter) {
                                    true -> TooltipCompat.setTooltipText(download, download.context
                                            .getString(R.string.fragment_episode_already_downloaded_hint))
                                    false -> TooltipCompat.setTooltipText(download, download.context
                                            .getString(R.string.fragment_episode_download_hint))
                                }

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

    private fun constructChapterCheckSingle(entryId: String, episode: Int, language: Language) = Single.fromCallable {
        mangaDao.countChaptersForEntry(entryId.toLong(), episode, language) > 0
    }
}
