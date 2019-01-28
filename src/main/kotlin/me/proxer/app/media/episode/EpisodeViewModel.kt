package me.proxer.app.media.episode

import io.reactivex.Single
import me.proxer.app.base.BaseViewModel
import me.proxer.app.util.extension.buildSingle

/**
 * @author Ruben Gees
 */
class EpisodeViewModel(private val entryId: String) : BaseViewModel<List<EpisodeRow>>() {

    override val dataSingle: Single<List<EpisodeRow>>
        get() = api.info.episodeInfo(entryId)
            .limit(Int.MAX_VALUE)
            .buildSingle()
            .map { info ->
                info.episodes
                    .asSequence()
                    .groupBy { it.number }
                    .map { (_, episodes) -> EpisodeRow(info.category, info.userProgress, info.lastEpisode, episodes) }
                    .toList()
            }
}
