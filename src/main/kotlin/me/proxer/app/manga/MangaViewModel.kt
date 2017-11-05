package me.proxer.app.manga

import com.hadisatrio.libs.android.viewmodelprovider.GeneratedProvider
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.BuildConfig
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.MainApplication.Companion.globalContext
import me.proxer.app.MainApplication.Companion.mangaDao
import me.proxer.app.base.BaseViewModel
import me.proxer.app.exception.NotFoundException
import me.proxer.app.util.ErrorUtils
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
import java.io.File
import kotlin.concurrent.write
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
@GeneratedProvider
class MangaViewModel(private val entryId: String, private val language: Language, episode: Int) :
        BaseViewModel<MangaChapterInfo>() {

    private companion object {
        private const val MAX_CACHE_SIZE = 1024L * 1024L * 256L
    }

    override val isLoginRequired = BuildConfig.STORE

    override val dataSingle: Single<MangaChapterInfo>
        get() = Single.fromCallable { validate() }
                .flatMapCompletable { cleanCompletable() }
                .andThen(entrySingle())
                .flatMap { localChapterSingle(it).onErrorResumeNext(remoteChapterSingle(it)) }

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

    private fun cleanCompletable() = Completable.fromAction {
        MangaLocks.cacheLock.write {
            val localDataInfoList = arrayListOf<LocalDataInfo>()
            val cacheDir = globalContext.cacheDir
            var overallSize = 0L

            File("$cacheDir/manga").list()?.forEach { entryDirectoryName ->
                File("$cacheDir/manga/$entryDirectoryName").list()?.forEach { chapterDirectoryName ->
                    val chapterDirectory = File("$cacheDir/manga/$entryDirectoryName/$chapterDirectoryName")
                    var size = 0L

                    chapterDirectory.walkTopDown().forEach {
                        if (it.isFile) {
                            size += it.length()
                            overallSize += size
                        }
                    }

                    localDataInfoList += LocalDataInfo(entryDirectoryName, chapterDirectoryName,
                            chapterDirectory.lastModified(), size)
                }
            }

            while (overallSize >= MAX_CACHE_SIZE) {
                localDataInfoList.minBy { it.lastModification }?.let {
                    File("$cacheDir/manga/${it.entryId}/${it.chapterId}").deleteRecursively()

                    overallSize -= it.size
                }
            }
        }
    }

    private fun entrySingle() = when (cachedEntryCore != null) {
        true -> Single.just(cachedEntryCore)
        false -> localEntrySingle().onErrorResumeNext(remoteEntrySingle())
    }

    private fun localEntrySingle() = Single.fromCallable {
        mangaDao.findEntry(entryId.toLong())?.toNonLocalEntryCore() ?: throw NotFoundException()
    }.doOnSuccess { cachedEntryCore = it }

    private fun remoteEntrySingle() = api.info().entryCore(entryId).buildSingle()

    private fun localChapterSingle(entry: EntryCore) = Single.fromCallable {
        val chapter = mangaDao.findChapter(entryId.toLong(), episode, language) ?: throw NotFoundException()
        val nonLocalChapter = chapter.toNonLocalChapter(mangaDao.getPagesForChapter(chapter.id)
                .map { it.toNonLocalPage() })

        MangaChapterInfo(nonLocalChapter, entry.name, entry.episodeAmount, true)
    }

    private fun remoteChapterSingle(entry: EntryCore) = api.manga().chapter(entryId, episode, language)
            .buildPartialErrorSingle(entry)
            .map { MangaChapterInfo(it, entry.name, entry.episodeAmount, false) }

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

    private class LocalDataInfo(val entryId: String, val chapterId: String, val lastModification: Long, val size: Long)
}
