package me.proxer.app.anime

import com.hadisatrio.libs.android.viewmodelprovider.GeneratedProvider
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.Singles
import io.reactivex.schedulers.Schedulers
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.anime.resolver.StreamResolutionResult
import me.proxer.app.anime.resolver.StreamResolverFactory
import me.proxer.app.base.BaseViewModel
import me.proxer.app.exception.AppRequiredException
import me.proxer.app.exception.StreamResolutionException
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.Validators
import me.proxer.app.util.data.ResettingMutableLiveData
import me.proxer.app.util.extension.buildOptionalSingle
import me.proxer.app.util.extension.buildPartialErrorSingle
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.app.util.extension.toAnimeStreamInfo
import me.proxer.app.util.extension.toMediaLanguage
import me.proxer.library.api.Endpoint
import me.proxer.library.entity.info.EntryCore
import me.proxer.library.enums.AnimeLanguage
import me.proxer.library.enums.Category
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
@GeneratedProvider
class AnimeViewModel(private val entryId: String, private val language: AnimeLanguage, episode: Int) :
        BaseViewModel<AnimeStreamInfo>() {

    override val dataSingle: Single<AnimeStreamInfo>
        get() = entrySingle().flatMap {
            Singles.zip(Single.just(it), streamSingle(it), { entry, streams ->
                AnimeStreamInfo(entry.name, entry.episodeAmount, streams.map {
                    it.toAnimeStreamInfo(StreamResolverFactory.resolverFor(it.hosterName) != null)
                })
            })
        }

    val resolutionResult = ResettingMutableLiveData<StreamResolutionResult>()
    val resolutionError = ResettingMutableLiveData<ErrorAction>()

    val bookmarkData = ResettingMutableLiveData<Unit?>()
    val bookmarkError = ResettingMutableLiveData<ErrorUtils.ErrorAction?>()

    var episode by Delegates.observable(episode, { _, old, new ->
        if (old != new) reload()
    })

    private var cachedEntryCore: EntryCore? = null

    private var resolverDisposable: Disposable? = null
    private var bookmarkDisposable: Disposable? = null

    override fun onCleared() {
        resolverDisposable?.dispose()
        bookmarkDisposable?.dispose()

        resolverDisposable = null
        bookmarkDisposable = null

        super.onCleared()
    }

    override fun load() {
        resolverDisposable?.dispose()

        super.load()
    }

    fun resolve(name: String, id: String) {
        resolverDisposable?.dispose()
        resolverDisposable = (StreamResolverFactory.resolverFor(name)?.resolve(id) ?: throw StreamResolutionException())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { isLoading.value = true }
                .doAfterTerminate { isLoading.value = false }
                .subscribeAndLogErrors({
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
                .subscribeAndLogErrors({
                    bookmarkError.value = null
                    bookmarkData.value = Unit
                }, {
                    bookmarkData.value = null
                    bookmarkError.value = ErrorUtils.handle(it)
                })
    }
}
