package me.proxer.app.media.episode

import android.app.Application
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.base.BaseViewModel
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.extension.buildSingle

/**
 * @author Ruben Gees
 */
class EpisodeViewModel(application: Application) : BaseViewModel<List<EpisodeRow>>(application) {

    lateinit var entryId: String

    private var disposable: Disposable? = null

    override fun onCleared() {
        disposable?.dispose()
        disposable = null

        super.onCleared()
    }

    override fun load() {
        disposable?.dispose()
        disposable = api.info().episodeInfo(entryId)
                .limit(Int.MAX_VALUE)
                .buildSingle()
                .map { info ->
                    info.episodes
                            .groupBy { it.number }
                            .map { EpisodeRow(info.category, info.userProgress, info.lastEpisode, it.value) }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    error.value = null
                    isLoading.value = true
                }
                .doAfterTerminate { isLoading.value = false }
                .subscribe({
                    error.value = null
                    data.value = it
                }, {
                    data.value = null
                    error.value = ErrorUtils.handle(it)
                })
    }
}