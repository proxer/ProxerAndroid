package com.proxerme.app.fragment.media

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import com.proxerme.app.activity.MangaActivity
import com.proxerme.app.adapter.media.EpisodeAdapter
import com.proxerme.app.entitiy.RichEpisode
import com.proxerme.app.fragment.framework.EasyPagingFragment
import com.proxerme.app.manager.SectionManager.Section
import com.proxerme.library.connection.info.entity.ListInfo
import com.proxerme.library.connection.info.request.ListInfoRequest

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class EpisodesFragment : EasyPagingFragment<RichEpisode, EpisodeAdapter.EpisodeAdapterCallback>() {

    companion object {

        const val ITEMS_ON_PAGE = Int.MAX_VALUE

        private const val ARGUMENT_ID = "id"

        fun newInstance(id: String): EpisodesFragment {
            return EpisodesFragment().apply {
                this.arguments = Bundle().apply {
                    this.putString(ARGUMENT_ID, id)
                }
            }
        }
    }

    override val itemsOnPage = ITEMS_ON_PAGE
    override val section = Section.EPISODES
    override val isSwipeToRefreshEnabled = false

    override lateinit var adapter: EpisodeAdapter
    override lateinit var layoutManager: LinearLayoutManager

    private lateinit var id: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        id = arguments.getString(ARGUMENT_ID)

        adapter = EpisodeAdapter(savedInstanceState)
        adapter.callback = object : EpisodeAdapter.EpisodeAdapterCallback() {
            override fun onLanguageClick(language: String, episode: RichEpisode) {
                if (episode.isAnime()) {
                    // TODO
                } else {
                    MangaActivity.navigateTo(activity, id, episode.number, episode.totalEpisodes,
                            language)
                }
            }
        }

        layoutManager = LinearLayoutManager(context)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        adapter.saveInstanceState(outState)
    }

    override fun constructPagedLoadingRequest(page: Int): LoadingRequest<Array<RichEpisode>> {
        return LoadingRequest(ListInfoRequest(id, page).withLimit(itemsOnPage),
                transformFunction = {
                    val listInfo = it as ListInfo

                    listInfo.episodes
                            .groupBy { it.number }
                            .values.map { RichEpisode(listInfo.userState, listInfo.lastEpisode, it) }
                            .toTypedArray()
                })
    }
}