package me.proxer.app.media.episode

import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.base.BaseViewModel
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.data.ResettingMutableLiveData
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.library.enums.Category
import me.proxer.library.enums.MediaLanguage

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
                    .sortedBy { it.number }
            }

    val bookmarkData = ResettingMutableLiveData<Unit?>()
    val bookmarkError = ResettingMutableLiveData<ErrorUtils.ErrorAction?>()

    private var bookmarkDisposable: Disposable? = null

    override fun onCleared() {
        bookmarkDisposable?.dispose()
        bookmarkDisposable = null

        super.onCleared()
    }

    fun bookmark(episode: Int, language: MediaLanguage, category: Category) {
        bookmarkDisposable?.dispose()
        bookmarkDisposable = Single.fromCallable { validators.validateLogin() }
            .flatMap { api.ucp.setBookmark(entryId, episode, language, category).buildSingle() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { isLoading.value = true }
            .doAfterTerminate { isLoading.value = false }
            .subscribeAndLogErrors(
                {
                    bookmarkError.value = null
                    bookmarkData.value = Unit
                },
                {
                    bookmarkData.value = null
                    bookmarkError.value = ErrorUtils.handle(it)
                }
            )
    }
}
