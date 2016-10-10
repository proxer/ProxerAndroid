package com.proxerme.app.fragment.framework

import android.os.Bundle
import android.support.annotation.CallSuper
import com.proxerme.library.connection.ProxerException
import com.proxerme.library.connection.ProxerRequest

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
abstract class LoadingFragment<T> : MainFragment() {

    private companion object {
        private const val LOADER_TAG = "loader"
        private const val FIRST_LOAD_STATE = "fragment_loading_state_first_load"
    }

    private var loader: RetainedLoadingFragment<T>? = null

    private var firstLoad = true

    open protected val canLoad: Boolean
        get() = !isLoading

    protected val isLoading: Boolean
        get() = loader?.isLoading() ?: false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.let {
            firstLoad = it.getBoolean(FIRST_LOAD_STATE)
        }

        initLoader()
    }

    override fun onResume() {
        super.onResume()

        loader?.setListener({ result ->
            firstLoad = false

            onLoadFinished(result)
        }, { result ->
            firstLoad = false

            onLoadFinishedWithError(result)
        })

        if (firstLoad) {
            load()
        }
    }

    override fun onDestroy() {
        loader?.removeListener()
        loader = null

        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBoolean(FIRST_LOAD_STATE, firstLoad)
    }

    @CallSuper
    open fun load() {
        if (canLoad && loader != null) {
            val loadingRequest = constructLoadingRequest()

            onLoadStarted()
            loader!!.load(*loadingRequest.requests,
                    transformFunction = loadingRequest.transformFunction,
                    zipFunction = loadingRequest.zipFunction)
        }
    }

    open fun reset() {
        loader?.cancel()
        firstLoad = false
        clear()

        load()
    }

    open protected fun onLoadStarted() {

    }

    open protected fun onLoadFinished(result: T) {

    }

    open protected fun onLoadFinishedWithError(result: ProxerException) {

    }

    protected abstract fun clear()

    protected abstract fun constructLoadingRequest(): LoadingRequest<T>

    @Suppress("UNCHECKED_CAST")
    private fun initLoader() {
        loader = childFragmentManager.findFragmentByTag(LOADER_TAG)
                as RetainedLoadingFragment<T>?

        if (loader == null) {
            loader = RetainedLoadingFragment<T>()

            childFragmentManager.beginTransaction()
                    .add(loader, LOADER_TAG)
                    .commitNow()
        }
    }

    class LoadingRequest<T> {

        val requests: Array<out ProxerRequest<*>>
        val transformFunction: ((result: Any?) -> T)?
        val zipFunction: ((partialResults: Array<Any?>) ->
        RetainedLoadingFragment.LoadingResult<T>)?

        constructor(vararg requests: ProxerRequest<*>,
                    transformFunction: ((result: Any?) -> T)? = null,
                    zipFunction: ((partialResults: Array<Any?>) ->
                    RetainedLoadingFragment.LoadingResult<T>)? = null) {
            this.requests = requests
            this.transformFunction = transformFunction
            this.zipFunction = zipFunction
        }
    }
}
