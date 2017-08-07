package me.proxer.app.manga

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.MainApplication.Companion.globalContext
import me.proxer.app.MainApplication.Companion.mangaDao
import me.proxer.app.base.BaseViewModel
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.Validators
import me.proxer.app.util.extension.buildOptionalSingle
import me.proxer.app.util.extension.buildPartialErrorSingle
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.toMediaLanguage
import me.proxer.library.api.Endpoint
import me.proxer.library.entitiy.info.EntryCore
import me.proxer.library.enums.Category
import me.proxer.library.enums.Language
import java.io.File
import kotlin.concurrent.write

/**
 * @author Ruben Gees
 */
class MangaViewModel(application: Application) : BaseViewModel<MangaChapterInfo>(application) {

    private companion object {
        private const val MAX_CACHE_SIZE = 1024L * 1024L * 256L
    }

    val bookmarkData = MutableLiveData<Unit?>()
    val bookmarkError = MutableLiveData<ErrorUtils.ErrorAction?>()

    lateinit var entryId: String
    lateinit var language: Language

    private var episode = 0
    private var cachedEntryCore: EntryCore? = null

    private var disposable: Disposable? = null
    private var bookmarkDisposable: Disposable? = null

    override fun onCleared() {
        disposable?.dispose()
        bookmarkDisposable?.dispose()

        disposable = null
        bookmarkDisposable = null

        super.onCleared()
    }

    override fun load() {
        cleanSingle()
                .andThen(entrySingle())
                .flatMap { localChapterSingle(it).onErrorResumeNext(remoteChapterSingle(it)) }
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

    fun setEpisode(value: Int, trigger: Boolean = true) {
        if (episode != value) {
            episode = value

            if (trigger) reload()
        }
    }

    fun markAsFinished() = updateUserState(api.info().markAsFinished(entryId))
    fun bookmark(episode: Int) = updateUserState(api.ucp().setBookmark(entryId, episode, language.toMediaLanguage(),
            Category.MANGA))

    private fun cleanSingle() = Completable.fromAction {
        MangaLocks.cacheLock.write {
            val localDataInfos = arrayListOf<LocalDataInfo>()
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

                    localDataInfos += LocalDataInfo(entryDirectoryName, chapterDirectoryName,
                            chapterDirectory.lastModified(), size)
                }
            }

            while (overallSize >= MAX_CACHE_SIZE) {
                localDataInfos.minBy { it.lastModification }?.let {
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
        mangaDao.findEntry(entryId.toLong())?.toNonLocalEntryCore() ?: throw RuntimeException()
    }.doOnSuccess { cachedEntryCore = it }

    private fun localChapterSingle(entry: EntryCore) = Single.fromCallable {
        val chapter = mangaDao.findChapter(entryId.toLong(), episode, language) ?: throw RuntimeException()
        val nonLocalChapter = chapter.toNonLocalChapter(mangaDao.getPagesForChapter(chapter.id).map { it.toNonLocalPage() })

        MangaChapterInfo(nonLocalChapter, entry.name, entry.episodeAmount, true)
    }

    private fun remoteEntrySingle() = api.info().entryCore(entryId).buildSingle()

    private fun remoteChapterSingle(entry: EntryCore) = api.manga().chapter(entryId, episode, language)
            .buildPartialErrorSingle(entry)
            .map { MangaChapterInfo(it, entry.name, entry.episodeAmount, false) }

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

    private class LocalDataInfo(val entryId: String, val chapterId: String, val lastModification: Long, val size: Long)
}
