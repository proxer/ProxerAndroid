package me.proxer.app.ucp.topten

import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.base.BaseViewModel
import me.proxer.app.ucp.topten.UcpTopTenViewModel.ZippedTopTenResult
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.Validators
import me.proxer.app.util.data.ResettingMutableLiveData
import me.proxer.app.util.data.UniqueQueue
import me.proxer.app.util.extension.buildOptionalSingle
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.library.entity.ucp.UcpTopTenEntry
import me.proxer.library.enums.Category

/**
 * @author Ruben Gees
 */
class UcpTopTenViewModel : BaseViewModel<ZippedTopTenResult>() {

    override val isLoginRequired = true

    override val dataSingle: Single<ZippedTopTenResult>
        get() = Single.fromCallable { Validators.validateLogin() }
            .flatMap { api.ucp().topTen().buildSingle() }
            .map {
                val animeList = it.filter { entry -> entry.category == Category.ANIME }
                val mangaList = it.filter { entry -> entry.category == Category.MANGA }

                ZippedTopTenResult(animeList, mangaList)
            }

    val itemDeletionError = ResettingMutableLiveData<ErrorUtils.ErrorAction?>()

    private val deletionQueue = UniqueQueue<UcpTopTenEntry>()
    private var deletionDisposable: Disposable? = null

    override fun onCleared() {
        deletionDisposable?.dispose()
        deletionDisposable = null

        super.onCleared()
    }

    fun addItemToDelete(item: UcpTopTenEntry) {
        deletionQueue.add(item)

        if (deletionDisposable?.isDisposed != false) {
            doItemDeletion()
        }
    }

    private fun doItemDeletion() {
        deletionDisposable?.dispose()

        deletionQueue.poll()?.let { item ->
            deletionDisposable = api.ucp().deleteFavorite(item.id)
                .buildOptionalSingle()
                .map {
                    data.value.let { currentData ->
                        if (currentData != null) {
                            val filteredAnimeEntries = currentData.animeEntries.filterNot { newItem -> newItem == item }
                            val filteredMangaEntries = currentData.mangaEntries.filterNot { newItem -> newItem == item }

                            ZippedTopTenResult(filteredAnimeEntries, filteredMangaEntries)
                        } else {
                            null
                        }
                    }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeAndLogErrors({
                    data.value = it

                    doItemDeletion()
                }, {
                    deletionQueue.clear()

                    itemDeletionError.value = ErrorUtils.handle(it)
                })
        }
    }

    data class ZippedTopTenResult(val animeEntries: List<UcpTopTenEntry>, val mangaEntries: List<UcpTopTenEntry>)
}
