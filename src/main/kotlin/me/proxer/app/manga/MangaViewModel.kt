package me.proxer.app.manga

import com.hadisatrio.libs.android.viewmodelprovider.GeneratedProvider
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.BuildConfig
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.base.BaseViewModel
import me.proxer.app.exception.PartialException
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.Utils
import me.proxer.app.util.Validators
import me.proxer.app.util.data.ResettingMutableLiveData
import me.proxer.app.util.extension.buildOptionalSingle
import me.proxer.app.util.extension.buildPartialErrorSingle
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.app.util.extension.toMediaLanguage
import me.proxer.library.api.Endpoint
import me.proxer.library.entity.info.EntryCore
import me.proxer.library.enums.Category
import me.proxer.library.enums.Language
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
@GeneratedProvider
class MangaViewModel(
    private val entryId: String,
    private val language: Language,
    episode: Int
) : BaseViewModel<MangaChapterInfo>() {

    private companion object {
        private val supportedExternalServers = arrayOf("www.webtoons.com", "www.lezhin.com")
    }

    override val isLoginRequired = BuildConfig.STORE

    override val dataSingle: Single<MangaChapterInfo>
        get() = Single.fromCallable { validate() }
            .flatMap<EntryCore> { entrySingle() }
            .flatMap { entry ->
                chapterSingle(entry).map { data ->
                    if (data.chapter.pages == null) {
                        val serverUrl = Utils.safelyParseAndFixUrl(data.chapter.server)

                        if (serverUrl != null && serverUrl.host() in supportedExternalServers) {
                            throw PartialException(MangaLinkException(data.chapter.title, serverUrl), entry)
                        } else {
                            throw PartialException(MangaNotAvailableException(), entry)
                        }
                    }

                    data
                }
            }

    val bookmarkData = ResettingMutableLiveData<Unit?>()
    val bookmarkError = ResettingMutableLiveData<ErrorUtils.ErrorAction?>()

    var episode by Delegates.observable(episode, { _, old, new ->
        if (old != new) reload()
    })

    private var cachedEntryCore: EntryCore? = null

    private var bookmarkDisposable: Disposable? = null

    override fun onCleared() {
        bookmarkDisposable?.dispose()
        bookmarkDisposable = null

        super.onCleared()
    }

    fun setEpisode(value: Int, trigger: Boolean = true) {
        if (episode != value) {
            episode = value

            if (trigger) reload()
        }
    }

    fun markAsFinished() = updateUserState(api.info().markAsFinished(entryId))
    fun bookmark(episode: Int) = updateUserState(api.ucp().setBookmark(entryId, episode, language.toMediaLanguage(),
        Category.MANGA))

    private fun entrySingle() = when (cachedEntryCore != null) {
        true -> Single.just(cachedEntryCore)
        false -> api.info().entryCore(entryId).buildSingle()
    }

    private fun chapterSingle(entry: EntryCore) = api.manga().chapter(entryId, episode, language)
        .buildPartialErrorSingle(entry)
        .map { MangaChapterInfo(it, entry.name, entry.episodeAmount) }

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
