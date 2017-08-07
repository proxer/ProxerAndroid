package me.proxer.app.manga

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.MainApplication.Companion.globalContext
import me.proxer.app.MainApplication.Companion.mangaDatabase
import me.proxer.app.manga.local.LocalMangaJob
import java.io.File
import kotlin.concurrent.write

/**
 * @author Ruben Gees
 */
class MangaCleanViewModel(application: Application?) : AndroidViewModel(application) {

    val data = MutableLiveData<Unit>()

    private var disposable: Disposable? = null

    override fun onCleared() {
        disposable?.dispose()
        disposable = null

        super.onCleared()
    }

    fun clean() {
        disposable?.dispose()
        disposable = Completable
                .fromAction {
                    LocalMangaJob.cancelAll()

                    mangaDatabase.clear()

                    MangaLocks.localLock.write {
                        File("${globalContext.filesDir}/manga").deleteRecursively()
                    }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { data.value = Unit }
    }
}
