package me.proxer.app.manga.local

import android.arch.lifecycle.MutableLiveData
import com.gojuno.koptional.toOptional
import com.hadisatrio.libs.android.viewmodelprovider.GeneratedProvider
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.toFlowable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.MainApplication.Companion.bus
import me.proxer.app.MainApplication.Companion.globalContext
import me.proxer.app.MainApplication.Companion.mangaDao
import me.proxer.app.R
import me.proxer.app.base.BaseViewModel
import me.proxer.app.manga.MangaLocks
import me.proxer.app.util.Validators
import me.proxer.app.util.extension.CompleteLocalMangaEntry
import me.proxer.app.util.extension.getQuantityString
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.concurrent.write
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
@GeneratedProvider
class LocalMangaViewModel(searchQuery: String?) : BaseViewModel<List<CompleteLocalMangaEntry>>() {

    override val isLoginRequired = true

    override val dataSingle: Single<List<CompleteLocalMangaEntry>>
        get() = Single.fromCallable { Validators.validateLogin() }
                .flatMap {
                    mangaDao.getEntries().toFlowable()
                            .map { it.toNonLocalEntryCore() to mangaDao.getChaptersForEntry(it.id) }
                            .filter { (entry, chapters) ->
                                chapters.isNotEmpty() && searchQuery.let {
                                    when {
                                        it != null && it.isNotBlank() -> entry.name.contains(it, true)
                                        else -> true
                                    }
                                }
                            }
                            .toList()
                }

    val jobInfo = MutableLiveData<String>()

    var searchQuery by Delegates.observable(searchQuery, { _, old, new ->
        if (old != new) reload()
    })

    private var deletionDisposable: Disposable? = null

    init {
        updateJobInfo()

        disposables += Observable
                .merge(
                        bus.register(LocalMangaJob.StartedEvent::class.java),
                        bus.register(LocalMangaJob.FinishedEvent::class.java)
                )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    updateJobInfo()
                    reload()
                }

        disposables += bus.register(LocalMangaJob.FailedEvent::class.java)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { updateJobInfo() }
    }

    override fun onCleared() {
        deletionDisposable?.dispose()
        deletionDisposable = null

        super.onCleared()
    }

    fun deleteChapter(chapter: LocalMangaChapter) {
        deletionDisposable?.dispose()
        deletionDisposable = Completable
                .fromAction {
                    mangaDao.deleteChapterAndEntryIfEmpty(chapter)

                    MangaLocks.localLock.write {
                        File("${globalContext.filesDir}/manga/${chapter.entryId}/${chapter.id}").deleteRecursively()
                    }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { reload() }
    }

    fun updateJobInfo() {
        disposables += Single
                .fromCallable {
                    val runningJobs = LocalMangaJob.countRunningJobs()
                    val scheduledJobs = LocalMangaJob.countScheduledJobs()
                    var message = ""

                    message += when (runningJobs > 0) {
                        true -> globalContext.getQuantityString(R.plurals.fragment_local_manga_chapters_downloading,
                                runningJobs)
                        false -> ""
                    }

                    message += when (runningJobs > 0 && scheduledJobs > 0) {
                        true -> "\n"
                        false -> ""
                    }

                    message += when (scheduledJobs > 0) {
                        true -> globalContext.getQuantityString(R.plurals.fragment_local_manga_chapters_scheduled,
                                scheduledJobs)
                        false -> ""
                    }

                    (if (message.isBlank()) null else message).toOptional()
                }
                .delaySubscription(50, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { value -> jobInfo.value = value.toNullable() }
    }
}
