package me.proxer.app.manga

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.hadisatrio.libs.android.viewmodelprovider.GeneratedProvider
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.MainApplication.Companion.globalContext
import me.proxer.app.MainApplication.Companion.mangaDao
import me.proxer.app.manga.local.LocalMangaJob
import me.proxer.app.util.extension.subscribeAndLogErrors
import java.io.File
import kotlin.concurrent.write

/**
 * @author Ruben Gees
 */
@GeneratedProvider
class MangaCleanViewModel : ViewModel() {

    val data = MutableLiveData<Unit>()

    private var dataDisposable: Disposable? = null

    override fun onCleared() {
        dataDisposable?.dispose()
        dataDisposable = null

        super.onCleared()
    }

    fun clean() {
        dataDisposable?.dispose()
        dataDisposable = Completable
                .fromAction {
                    LocalMangaJob.cancelAll()

                    mangaDao.clear()

                    MangaLocks.localLock.write {
                        File("${globalContext.filesDir}/manga").deleteRecursively()
                    }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeAndLogErrors { data.value = Unit }
    }
}
