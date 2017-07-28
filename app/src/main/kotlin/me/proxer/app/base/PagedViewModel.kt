package me.proxer.app.base

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.extension.buildSingle
import me.proxer.library.api.PagingLimitEndpoint
import me.proxer.library.entitiy.ProxerIdItem

/**
 * @author Ruben Gees
 */
abstract class PagedViewModel<T>(application: Application) : BaseViewModel<List<T>>(application) {

    val refreshError = MutableLiveData<ErrorAction?>()

    private var hasReachedEnd = false

    override abstract val endpoint: PagingLimitEndpoint<List<T>>
    abstract val itemsOnPage: Int

    override fun load() {
        load(data.value?.size?.div(itemsOnPage) ?: 0)
    }

    override fun loadIfPossible() {
        if (!hasReachedEnd) {
            super.loadIfPossible()
        }
    }

    override fun refresh() {
        load(0)
    }

    override fun reload() {
        refreshError.value = null

        super.reload()
    }

    open protected fun areItemsTheSame(old: T, new: T) = when {
        old is ProxerIdItem && new is ProxerIdItem -> old.id == new.id
        else -> old == new
    }

    open protected fun load(page: Int) {
        disposable?.dispose()
        disposable = endpoint
                .page(page)
                .limit(itemsOnPage)
                .buildSingle()
                .subscribeOn(Schedulers.io())
                .map { it.apply { validate() } }
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
                .doAfterSuccess { newData ->
                    hasReachedEnd = newData.size < itemsOnPage
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
