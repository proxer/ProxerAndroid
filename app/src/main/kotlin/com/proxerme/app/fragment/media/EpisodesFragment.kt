package com.proxerme.app.fragment.media

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.proxerme.app.R
import com.proxerme.app.activity.AnimeActivity
import com.proxerme.app.activity.MangaActivity
import com.proxerme.app.adapter.media.EpisodeAdapter
import com.proxerme.app.entitiy.RichEpisode
import com.proxerme.app.fragment.framework.SingleLoadingFragment
import com.proxerme.app.manager.SectionManager.Section
import com.proxerme.app.task.ProxerLoadingTask
import com.proxerme.app.task.framework.MappedTask
import com.proxerme.app.task.framework.Task
import com.proxerme.app.util.bindView
import com.proxerme.library.connection.info.entity.ListInfo
import com.proxerme.library.connection.info.request.ListInfoRequest

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class EpisodesFragment : SingleLoadingFragment<String, Array<RichEpisode>>() {

    companion object {
        private const val ARGUMENT_ID = "id"

        fun newInstance(id: String): EpisodesFragment {
            return EpisodesFragment().apply {
                this.arguments = Bundle().apply {
                    this.putString(ARGUMENT_ID, id)
                }
            }
        }
    }

    override val section = Section.EPISODES
    override val isSwipeToRefreshEnabled = false

    private lateinit var adapter: EpisodeAdapter

    private val list: RecyclerView by bindView(R.id.list)

    private val id: String
        get() = arguments.getString(ARGUMENT_ID)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = EpisodeAdapter()
        adapter.callback = object : EpisodeAdapter.EpisodeAdapterCallback() {
            override fun onLanguageClick(language: String, episode: RichEpisode) {
                if (episode.isAnime()) {
                    AnimeActivity.navigateTo(activity, id, episode.number, language,
                            episode.totalEpisodes)
                } else {
                    MangaActivity.navigateTo(activity, id, episode.number, language,
                            episode.totalEpisodes)
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

        list.layoutManager = LinearLayoutManager(context)
        list.adapter = adapter
    }

    override fun onDestroyView() {
        list.adapter = null
        list.layoutManager = null

        super.onDestroyView()
    }

    override fun present(data: Array<RichEpisode>) {
        if (data.isEmpty()) {
            showError(getString(R.string.error_no_data_episodes), null)
        } else {
            adapter.replace(data)
        }
    }

    override fun constructTask(): Task<String, Array<RichEpisode>> {
        return MappedTask(ProxerLoadingTask<String, ListInfo>({
            ListInfoRequest(it, 0).withLimit(Int.MAX_VALUE)
        }), { listInfo ->
            listInfo.episodes.groupBy { it.number }
                    .values.map { RichEpisode(listInfo.userState, listInfo.lastEpisode, it) }
                    .toTypedArray()
        })
    }

    override fun constructInput(): String {
        return id
    }
}
