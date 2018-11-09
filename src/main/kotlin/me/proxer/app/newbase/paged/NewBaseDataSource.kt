package me.proxer.app.newbase.paged

import androidx.lifecycle.MutableLiveData
import androidx.paging.PageKeyedDataSource
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.R
import me.proxer.app.newbase.NetworkState
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.extension.subscribeAndLogErrors

/**
 * @author Ruben Gees
 */
abstract class NewBaseDataSource<T> : PageKeyedDataSource<Int, T>() {

    val networkState = MutableLiveData<NetworkState>()

    protected var disposable: Disposable? = null
    protected var retry: (() -> Unit)? = null

    override fun loadInitial(params: LoadInitialParams<Int>, callback: LoadInitialCallback<Int, T>) {
        disposable = prepareCall(0, params.requestedLoadSize)
            .subscribeOn(Schedulers.io())
            .doOnSubscribe { retry = null }
            .doOnSubscribe { networkState.postValue(NetworkState.Loading) }
            .doOnSuccess { networkState.postValue(NetworkState.Idle) }
            .doOnError { networkState.postValue(NetworkState.Error(ErrorUtils.handle(it))) }
            .doOnError { retry = { loadInitial(params, callback) } }
            .subscribeAndLogErrors {
                if (it.isEmpty()) {
                    val errorAction = ErrorAction(R.string.error_no_data, ErrorAction.ACTION_MESSAGE_HIDE)

                    networkState.postValue(NetworkState.Error(errorAction))
                } else {
                    val nextPage = when {
                        it.size >= params.requestedLoadSize -> 1
                        else -> null
                    }

                    callback.onResult(it, null, nextPage)
                }
            }
    }

    override fun loadAfter(params: LoadParams<Int>, callback: LoadCallback<Int, T>) {
        disposable = prepareCall(params.key, params.requestedLoadSize)
            .subscribeOn(Schedulers.io())
            .doOnSubscribe { retry = null }
            .doOnSubscribe { networkState.postValue(NetworkState.Loading) }
            .doOnSuccess { networkState.postValue(NetworkState.Idle) }
            .doOnError { networkState.postValue(NetworkState.Error(ErrorUtils.handle(it))) }
            .doOnError { retry = { loadAfter(params, callback) } }
            .subscribeAndLogErrors {
                val nextPage = when {
                    it.size >= params.requestedLoadSize -> params.key + 1
                    else -> null
                }

                callback.onResult(it, nextPage)
            }
    }

    override fun loadBefore(params: LoadParams<Int>, callback: LoadCallback<Int, T>) {
        disposable = prepareCall(params.key, params.requestedLoadSize)
            .subscribeOn(Schedulers.io())
            .doOnSubscribe { retry = null }
            .doOnSubscribe { networkState.postValue(NetworkState.Loading) }
            .doOnSuccess { networkState.postValue(NetworkState.Idle) }
            .doOnError { networkState.postValue(NetworkState.Error(ErrorUtils.handle(it))) }
            .doOnError { retry = { loadBefore(params, callback) } }
            .subscribeAndLogErrors {
                val nextPage = when {
                    it.size < params.requestedLoadSize -> null
                    params.key >= 1 -> params.key - 1
                    else -> null
                }

                callback.onResult(it, nextPage)
            }
    }

    override fun invalidate() {
        disposable?.dispose()
        disposable = null

        super.invalidate()
    }

    fun retry() {
        retry?.invoke()
    }

    protected abstract fun prepareCall(page: Int, loadSize: Int): Single<List<T>>
}
