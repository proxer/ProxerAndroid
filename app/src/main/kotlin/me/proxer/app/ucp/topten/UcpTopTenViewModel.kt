package me.proxer.app.ucp.topten

import android.app.Application
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.MainApplication.Companion.api
import me.proxer.app.base.BaseViewModel
import me.proxer.app.ucp.topten.UcpTopTenViewModel.ZippedTopTenResult
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.data.ResettingMutableLiveData
import me.proxer.app.util.data.UniqueQueue
import me.proxer.app.util.extension.buildOptionalSingle
import me.proxer.app.util.extension.buildSingle
import me.proxer.library.entitiy.ucp.UcpTopTenEntry
import me.proxer.library.enums.Category

/**
 * @author Ruben Gees
 */
class UcpTopTenViewModel(application: Application) : BaseViewModel<ZippedTopTenResult>(application) {

    override val isLoginRequired = true

    override val dataSingle: Single<ZippedTopTenResult>
        get() = api.ucp().topTen()
                .buildSingle()
                .map {
                    val animeList = it.filter { it.category == Category.ANIME }
                    val mangaList = it.filter { it.category == Category.MANGA }

                    ZippedTopTenResult(animeList, mangaList)
                }

    val itemRemovalError = ResettingMutableLiveData<ErrorUtils.ErrorAction?>()

    private val removalQueue = UniqueQueue<UcpTopTenEntry>()
    private var removalDisposable: Disposable? = null

    override fun onCleared() {
        removalDisposable?.dispose()
        removalDisposable = null

        super.onCleared()
    }

    fun addItemToRemove(item: UcpTopTenEntry) {
        removalQueue.add(item)

        doItemRemoval()
    }

    private fun doItemRemoval() {
        removalDisposable?.dispose()

        removalQueue.poll()?.let { item ->
            removalDisposable = api.ucp().deleteFavorite(item.id)
                    .buildOptionalSingle()
                    .map {
                        data.value.let { currentData ->
                            if (currentData != null) {
                                val filteredAnimeEntries = currentData.animeEntries.filterNot { it == item }
                                val filteredMangaEntries = currentData.mangaEntries.filterNot { it == item }

                                ZippedTopTenResult(filteredAnimeEntries, filteredMangaEntries)
                            } else {
                                null
                            }
                        }
                    }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        data.value = it

                        doItemRemoval()
                    }, {
                        removalQueue.clear()

                        itemRemovalError.value = ErrorUtils.handle(it)
                    })
        }
    }

    class ZippedTopTenResult(val animeEntries: List<UcpTopTenEntry>, val mangaEntries: List<UcpTopTenEntry>)
}
