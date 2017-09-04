package me.proxer.app.media.episode

import android.app.Application
import io.reactivex.Single
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.base.BaseViewModel
import me.proxer.app.util.extension.buildSingle
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class EpisodeViewModel(application: Application) : BaseViewModel<List<EpisodeRow>>(application) {

    override val dataSingle: Single<List<EpisodeRow>>
        get() = api.info().episodeInfo(entryId)
                .limit(Int.MAX_VALUE)
                .buildSingle()
                .map { info ->
                    info.episodes
                            .groupBy { it.number }
                            .map { EpisodeRow(info.category, info.userProgress, info.lastEpisode, it.value) }
                }

    var entryId by Delegates.notNull<String>()
}
