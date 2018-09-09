package me.proxer.app.anime

import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.zipWith
import io.reactivex.schedulers.Schedulers
import me.proxer.app.BuildConfig
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.MainApplication.Companion.bus
import me.proxer.app.MainApplication.Companion.globalContext
import me.proxer.app.anime.resolver.StreamResolutionResult
import me.proxer.app.anime.resolver.StreamResolverFactory
import me.proxer.app.base.BaseViewModel
import me.proxer.app.exception.AgeConfirmationRequiredException
import me.proxer.app.exception.AppRequiredException
import me.proxer.app.exception.StreamResolutionException
import me.proxer.app.settings.AgeConfirmationEvent
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.ErrorUtils.ErrorAction.ButtonAction
import me.proxer.app.util.Validators
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.ResettingMutableLiveData
import me.proxer.app.util.extension.buildOptionalSingle
import me.proxer.app.util.extension.buildPartialErrorSingle
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.isAgeRestricted
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
class AnimeViewModel(
    private val entryId: String,
    private val language: AnimeLanguage,
    episode: Int
) : BaseViewModel<AnimeStreamInfo>() {

    override val isLoginRequired = BuildConfig.STORE

    override val dataSingle: Single<AnimeStreamInfo>
        get() = Single.fromCallable { validate() }
            .flatMap { entrySingle() }
            .doOnSuccess {
                if (it.isAgeRestricted && !PreferenceHelper.isAgeRestrictedMediaAllowed(globalContext)) {
                    throw AgeConfirmationRequiredException()
                }
            }
            .flatMap {
                Single.just(it).zipWith(streamSingle(it)) { entry, streams ->
                    AnimeStreamInfo(entry.name, entry.episodeAmount, streams.map { stream ->
                        val resolver = StreamResolverFactory.resolverFor(stream.hosterName)
                        val internalPlayerOnly = resolver?.internalPlayerOnly ?: false

                        stream.toAnimeStreamInfo(resolver != null, internalPlayerOnly)
                    })
                }
            }

    val resolutionResult = ResettingMutableLiveData<StreamResolutionResult>()
    val resolutionError = ResettingMutableLiveData<ErrorAction>()

    val userStateData = ResettingMutableLiveData<Unit?>()
    val userStateError = ResettingMutableLiveData<ErrorUtils.ErrorAction?>()

    var episode by Delegates.observable(episode) { _, old, new ->
        if (old != new) reload()
    }

    private var cachedEntryCore: EntryCore? = null

    private var resolverDisposable: Disposable? = null
    private var userStateDisposable: Disposable? = null

    init {
        disposables += bus.register(AgeConfirmationEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                // TODO: Simplify once proguard does not crash on this.
                val safeValue = error.value

                if (safeValue != null && safeValue.buttonAction == ButtonAction.AGE_CONFIRMATION) {
                    reload()
                }
            }
    }

    override fun onCleared() {
        resolverDisposable?.dispose()
        userStateDisposable?.dispose()

        resolverDisposable = null
        userStateDisposable = null

        super.onCleared()
    }

    override fun load() {
        resolverDisposable?.dispose()

        super.load()
    }

    fun resolve(name: String, id: String) {
        resolverDisposable?.dispose()
        resolverDisposable = (StreamResolverFactory.resolverFor(name)?.resolve(id)
            ?: throw StreamResolutionException())
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

    fun bookmark(episode: Int) = updateUserState(
        api.ucp().setBookmark(entryId, episode, language.toMediaLanguage(), Category.ANIME)
    )

    private fun entrySingle(): Single<EntryCore> {
        val safeCachedEntryCore = cachedEntryCore

        return when (safeCachedEntryCore != null) {
            true -> Single.just(safeCachedEntryCore)
            false -> api.info().entryCore(entryId).buildSingle()
        }.doOnSuccess { cachedEntryCore = it }
    }

    private fun streamSingle(entry: EntryCore) = api.anime().streams(entryId, episode, language)
        .includeProxerStreams(true)
        .buildPartialErrorSingle(entry)
        .map { it.filterNot { stream -> StreamResolverFactory.resolverFor(stream.hosterName)?.ignore == true } }

    private fun updateUserState(endpoint: Endpoint<Void>) {
        userStateDisposable?.dispose()
        userStateDisposable = Single.fromCallable { Validators.validateLogin() }
            .flatMap { endpoint.buildOptionalSingle() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeAndLogErrors({
                userStateError.value = null
                userStateData.value = Unit
            }, {
                userStateData.value = null
                userStateError.value = ErrorUtils.handle(it)
            })
    }
}
