package me.proxer.app.media.episode

import com.hadisatrio.libs.android.viewmodelprovider.GeneratedProvider
import io.reactivex.Single
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.base.BaseViewModel
import me.proxer.app.util.extension.buildSingle

/**
 * @author Ruben Gees
 */
@GeneratedProvider
class EpisodeViewModel(private val entryId: String) : BaseViewModel<List<EpisodeRow>>() {

    override val dataSingle: Single<List<EpisodeRow>>
        get() = api.info().episodeInfo(entryId)
                .limit(Int.MAX_VALUE)
                .buildSingle()
                .map { info ->
                    info.episodes
                            .groupBy { it.number }
                            .map { EpisodeRow(info.category, info.userProgress, info.lastEpisode, it.value) }
                }
}
