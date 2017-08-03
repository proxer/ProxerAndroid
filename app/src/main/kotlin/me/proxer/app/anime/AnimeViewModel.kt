package me.proxer.app.anime

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.anime.resolver.StreamResolutionException
import me.proxer.app.anime.resolver.StreamResolutionResult
import me.proxer.app.anime.resolver.StreamResolverFactory
import me.proxer.app.base.BaseViewModel
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.Validators
import me.proxer.app.util.extension.*
import me.proxer.library.api.Endpoint
import me.proxer.library.entitiy.anime.Stream
import me.proxer.library.entitiy.info.EntryCore
import me.proxer.library.enums.AnimeLanguage
import me.proxer.library.enums.Category

/**
 * @author Ruben Gees
 */
class AnimeViewModel(application: Application) : BaseViewModel<AnimeStreamInfo>(application) {

    val resolutionResult = MutableLiveData<StreamResolutionResult>()
    val resolutionError = MutableLiveData<ErrorAction>()

    val bookmarkData = MutableLiveData<Unit?>()
    val bookmarkError = MutableLiveData<ErrorUtils.ErrorAction?>()

    lateinit var entryId: String
    lateinit var language: AnimeLanguage

    private var episode = 0
    private var cachedEntryCore: EntryCore? = null

    private val dataSingle
        get() = entrySingle().flatMap {
            Single.zip(Single.just(it), streamSingle(it),
                    BiFunction<EntryCore, List<Stream>, AnimeStreamInfo> { entry, streams ->
                        AnimeStreamInfo(entry.name, entry.episodeAmount, streams.map {
                            it.toAnimeStreamInfo(StreamResolverFactory.resolverFor(it.hosterName) != null)
                        })
                    })
        }

    private var disposable: Disposable? = null
    private var resolverDisposable: Disposable? = null
    private var bookmarkDisposable: Disposable? = null

    override fun onCleared() {
        disposable?.dispose()
        resolverDisposable?.dispose()
        bookmarkDisposable?.dispose()

        disposable = null
        resolverDisposable = null
        bookmarkDisposable = null

        super.onCleared()
    }

    override fun load() {
        disposable?.dispose()
        resolverDisposable?.dispose()
        dataSingle.subscribeOn(Schedulers.io())
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

    fun resolve(name: String, id: String) {
        resolverDisposable?.dispose()
        resolverDisposable = (StreamResolverFactory.resolverFor(name)?.resolve(id) ?: throw StreamResolutionException())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { isLoading.value = true }
                .doAfterTerminate { isLoading.value = false }
                .subscribe({
                    resolutionError.value = null
                    resolutionResult.value = it
                }, {
                    resolutionResult.value = null
                    resolutionError.value = when (it) {
                        is AppRequiredException -> AppRequiredErrorAction(it.name, it.appPackage)
                        else -> ErrorUtils.handle(it)
                    }
                })
    }

    fun setEpisode(value: Int, trigger: Boolean = true) {
        if (episode != value) {
            episode = value

            if (trigger) reload()
        }
    }

    fun markAsFinished() = updateUserState(api.info().markAsFinished(entryId))
    fun bookmark(episode: Int) = updateUserState(api.ucp().setBookmark(entryId, episode, language.toMediaLanguage(),
            Category.ANIME))

    private fun entrySingle() = when (cachedEntryCore != null) {
        true -> Single.just(cachedEntryCore)
        false -> api.info().entryCore(entryId).buildSingle()
    }.doOnSuccess { cachedEntryCore = it }

    private fun streamSingle(entry: EntryCore) = api.anime().streams(entryId, episode, language)
            .includeProxerStreams(true)
            .buildPartialErrorSingle(entry)

    private fun updateUserState(endpoint: Endpoint<Void>) {
        bookmarkDisposable?.dispose()
        bookmarkDisposable = Single.fromCallable { Validators.validateLogin() }
                .flatMap { endpoint.buildOptionalSingle() }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    bookmarkError.value = null
                    bookmarkData.value = Unit
                }, {
                    bookmarkData.value = null
                    bookmarkError.value = ErrorUtils.handle(it)
                })
    }
}
