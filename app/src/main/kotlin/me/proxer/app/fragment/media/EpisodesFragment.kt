package me.proxer.app.fragment.media

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.rubengees.ktask.util.TaskBuilder
import me.proxer.app.R
import me.proxer.app.activity.AnimeActivity
import me.proxer.app.activity.MangaActivity
import me.proxer.app.activity.MediaActivity
import me.proxer.app.adapter.media.EpisodeAdapter
import me.proxer.app.application.MainApplication.Companion.api
import me.proxer.app.entity.EpisodeRow
import me.proxer.app.fragment.base.LoadingFragment
import me.proxer.app.task.asyncProxerTask
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.extension.bindView
import me.proxer.app.util.extension.toAnimeLanguage
import me.proxer.app.util.extension.toGeneralLanguage
import me.proxer.library.api.ProxerCall
import me.proxer.library.entitiy.info.EpisodeInfo
import me.proxer.library.enums.Category
import me.proxer.library.enums.MediaLanguage
import org.jetbrains.anko.bundleOf

/**
 * @author Ruben Gees
 */
class EpisodesFragment : LoadingFragment<ProxerCall<EpisodeInfo>, List<EpisodeRow>>() {

    companion object {
        fun newInstance(): EpisodesFragment {
            return EpisodesFragment().apply {
                arguments = bundleOf()
            }
        }
    }

    override val isSwipeToRefreshEnabled = false

    private val mediaActivity
        get() = activity as MediaActivity

    private lateinit var adapter: EpisodeAdapter

    private val id: String
        get() = mediaActivity.id
    private val name: String?
        get() = mediaActivity.name
    private val category: Category?
        get() = mediaActivity.category

    private val list: RecyclerView by bindView(R.id.list)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = EpisodeAdapter(savedInstanceState)
        adapter.callback = object : EpisodeAdapter.EpisodeAdapterCallback() {
            override fun onLanguageClick(language: MediaLanguage, episode: EpisodeRow) {
                when (episode.category) {
                    Category.ANIME -> {
                        AnimeActivity.navigateTo(activity, id, episode.number, language.toAnimeLanguage(), name,
                                episode.episodeAmount)
                    }
                    Category.MANGA -> {
                        MangaActivity.navigateTo(activity, id, episode.number, language.toGeneralLanguage(), name,
                                episode.episodeAmount)
                    }
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_episodes, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        list.setHasFixedSize(true)
        list.layoutManager = LinearLayoutManager(context)
        list.adapter = adapter
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        adapter.saveInstanceState(outState)
    }

    override fun onSuccess(result: List<EpisodeRow>) {
        adapter.replace(result)

        super.onSuccess(result)
    }

    override fun showContent() {
        super.showContent()

        if (adapter.isEmpty()) {
            showError(when (category) {
                Category.ANIME, null -> R.string.error_no_data_episodes
                Category.MANGA -> R.string.error_no_data_chapters
            }, ErrorUtils.ErrorAction.ACTION_MESSAGE_HIDE)
        }
    }

    override fun constructTask() = TaskBuilder.asyncProxerTask<EpisodeInfo>()
            .map { info ->
                info.episodes.groupBy { it.number }
                        .map { EpisodeRow(info.category, info.userProgress, info.lastEpisode, it.value) }
            }.build()

    override fun constructInput() = api.info().episodeInfo(id)
            .limit(Int.MAX_VALUE)
            .build()
}
