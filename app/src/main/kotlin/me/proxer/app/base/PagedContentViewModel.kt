package me.proxer.app.base

import android.app.Application
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.extension.buildSingle
import me.proxer.library.api.PagingLimitEndpoint
import me.proxer.library.entitiy.ProxerIdItem

/**
 * @author Ruben Gees
 */
abstract class PagedContentViewModel<T>(application: Application) : PagedViewModel<T>(application) {

    private var disposable: Disposable? = null

    abstract protected val endpoint: PagingLimitEndpoint<List<T>>

    override fun onCleared() {
        disposable?.dispose()
        disposable = null

        super.onCleared()
    }

    open protected fun areItemsTheSame(old: T, new: T) = when {
        old is ProxerIdItem && new is ProxerIdItem -> old.id == new.id
        else -> old == new
    }

    override fun load(page: Int) {
        disposable?.dispose()
        disposable = Single.fromCallable { validate() }
                .flatMap { endpoint.page(page).limit(itemsOnPage).buildSingle() }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterSuccess { newData -> hasReachedEnd = newData.size < itemsOnPage }
                .observeOn(Schedulers.io())
                .map { newData ->
                    data.value.let { existingData ->
                        when (existingData) {
                            null -> newData
                            else -> when (page) {
                                0 -> newData + existingData.filter { item ->
                                    newData.find { oldItem -> areItemsTheSame(oldItem, item) } == null
                                }
                                else -> existingData.filter { item ->
                                    newData.find { oldItem -> areItemsTheSame(oldItem, item) } == null
                                } + newData
                            }
                        }
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    refreshError.value = null
                    error.value = null
                    isLoading.value = true
                }
                .doAfterTerminate {
                    isLoading.value = false
                }
                .subscribe({
                    refreshError.value = null
                    error.value = null
                    data.value = it
                }, {
                    if (page == 0 && data.value?.size ?: 0 > 0) {
                        refreshError.value = ErrorUtils.handle(it)
                    } else {
                        error.value = ErrorUtils.handle(it)
                    }
                })
    }
}
