package me.proxer.app.manga

import android.app.Application
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.MainApplication
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.MainApplication.Companion.mangaDao
import me.proxer.app.base.BaseViewModel
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.extension.buildPartialErrorSingle
import me.proxer.app.util.extension.buildSingle
import me.proxer.library.entitiy.info.EntryCore
import me.proxer.library.enums.Language

/**
 * @author Ruben Gees
 */
class MangaViewModel(application: Application) : BaseViewModel<MangaChapterInfo>(application) {

    lateinit var entryId: String
    lateinit var language: Language

    private var episode = 0

    private val entrySingle by lazy { localEntrySingle().onErrorResumeNext(remoteEntrySingle()).cache() }

    private var disposable: Disposable? = null

    override fun onCleared() {
        disposable?.dispose()
        disposable = null

        super.onCleared()
    }

    override fun load() {
        entrySingle
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

    private fun localEntrySingle() = Single.fromCallable {
        mangaDao.findEntry(entryId.toLong())?.toNonLocalEntryCore() ?: throw RuntimeException()
    }

    private fun localChapterSingle(entry: EntryCore) = Single.fromCallable {
        val chapter = mangaDao.findChapter(entryId.toLong(), episode, language) ?: throw RuntimeException()
        val nonLocalChapter = chapter.toNonLocalChapter(mangaDao.getPages(chapter.id).map { it.toNonLocalPage() })

        MangaChapterInfo(nonLocalChapter, entry.name, entry.episodeAmount, true)
    }

    private fun remoteEntrySingle() = MainApplication.api.info().entryCore(entryId).buildSingle()

    private fun remoteChapterSingle(entry: EntryCore) = api.manga().chapter(entryId, episode, language)
            .buildPartialErrorSingle(entry)
            .map { MangaChapterInfo(it, entry.name, entry.episodeAmount, false) }
}
