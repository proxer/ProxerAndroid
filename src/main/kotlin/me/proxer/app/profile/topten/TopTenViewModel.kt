package me.proxer.app.profile.topten

import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import me.proxer.app.base.BaseViewModel
import me.proxer.app.profile.topten.TopTenViewModel.ZippedTopTenResult
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.data.ResettingMutableLiveData
import me.proxer.app.util.data.UniqueQueue
import me.proxer.app.util.extension.buildOptionalSingle
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.app.util.extension.toLocalEntry
import me.proxer.app.util.extension.toLocalEntryUcp
import me.proxer.library.enums.Category

/**
 * @author Ruben Gees
 */
class TopTenViewModel(
    private val userId: String?,
    private val username: String?
) : BaseViewModel<ZippedTopTenResult>() {

    override val dataSingle: Single<ZippedTopTenResult>
        get() = when (storageHelper.user?.matches(userId, username) == true) {
            true -> api.ucp.topTen().buildSingle()
                .map { entries -> entries.map { it.toLocalEntryUcp() } }
                .map {
                    val animeList = it.filter { entry -> entry.category == Category.ANIME }
                    val mangaList = it.filter { entry -> entry.category == Category.MANGA }

                    ZippedTopTenResult(animeList, mangaList)
                }
            false -> {
                val includeHentai = preferenceHelper.isAgeRestrictedMediaAllowed && storageHelper.isLoggedIn

                Singles.zip(
                    partialSingle(includeHentai, Category.ANIME),
                    partialSingle(includeHentai, Category.MANGA),
                    zipper = { animeEntries, mangaEntries ->
                        ZippedTopTenResult(animeEntries, mangaEntries)
                    }
                )
            }
        }

    val itemDeletionError = ResettingMutableLiveData<ErrorUtils.ErrorAction?>()

    private val deletionQueue = UniqueQueue<LocalTopTenEntry>()
    private var deletionDisposable: Disposable? = null

    init {
        disposables += storageHelper.isLoggedInObservable.subscribe { reload() }
    }

    override fun onCleared() {
        deletionDisposable?.dispose()
        deletionDisposable = null

        super.onCleared()
    }

    fun addItemToDelete(item: LocalTopTenEntry) {
        deletionQueue.add(item)

        if (deletionDisposable?.isDisposed != false) {
            doItemDeletion()
        }
    }

    private fun partialSingle(includeHentai: Boolean, category: Category) = api.user.topTen(userId, username)
        .includeHentai(includeHentai)
        .category(category)
        .buildSingle()
        .map { entries -> entries.map { it.toLocalEntry() } }

    private fun doItemDeletion() {
        deletionDisposable?.dispose()

        deletionQueue.poll()?.let { item ->
            deletionDisposable = api.ucp.deleteFavorite(item.id)
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

    data class ZippedTopTenResult(val animeEntries: List<LocalTopTenEntry>, val mangaEntries: List<LocalTopTenEntry>)
}
