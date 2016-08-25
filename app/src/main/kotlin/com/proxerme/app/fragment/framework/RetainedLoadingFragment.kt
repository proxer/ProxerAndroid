package com.proxerme.app.fragment.framework

import android.os.Bundle
import android.support.v4.app.Fragment
import com.proxerme.app.application.MainApplication
import com.proxerme.library.connection.ProxerCall
import com.proxerme.library.connection.ProxerException
import com.proxerme.library.connection.ProxerRequest
import java.util.*

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class RetainedLoadingFragment<T>() : Fragment() {

    private var successCallback: ((T) -> Unit)? = null
    private var errorCallback: ((ProxerException) -> Unit)? = null

    private var result: LoadingResult<T>? = null
    private var exception: ProxerException? = null

    private val calls = LinkedList<ProxerCall>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        retainInstance = true
    }

    override fun onDestroy() {
        cancelAndClean()

        super.onDestroy()
    }

    fun setListener(successCallback: (T) -> Unit,
                    errorCallback: (ProxerException) -> Unit): RetainedLoadingFragment<T> {
        this.successCallback = successCallback
        this.errorCallback = errorCallback

        deliverAndCleanIfPossible()

        return this
    }

    fun removeListener() {
        successCallback = null
        errorCallback = null
    }

    @Suppress("UNCHECKED_CAST")
    fun load(vararg requests: ProxerRequest<*>,
             zipFunction: ((partialResults: Array<Any?>) -> LoadingResult<T>)? = null) {
        if (requests.isEmpty()) {
            throw RuntimeException("You have to pass at least one request")
        }

        cancelAndClean()

        val partialResults: Array<Any?> = arrayOfNulls(requests.size)

        requests.forEach {
            calls.add(MainApplication.proxerConnection.execute(it, { successResult ->
                partialResults[requests.indexOf(it)] = successResult

                if (partialResults.size == requests.size) {
                    if (partialResults.size > 1) {
                        if (zipFunction == null) {
                            throw RuntimeException("You have to provide a zip function for" +
                                    "multiple request loading")
                        } else {
                            result = zipFunction.invoke(partialResults)
                        }
                    } else {
                        result = LoadingResult(successResult as T)
                    }
                }

                deliverAndCleanIfPossible()
            }, { errorResult ->
                exception = errorResult

                deliverAndCleanIfPossible()
            }))

            return
        }
    }

    fun cancel() {
        cancelAndClean()
    }

    fun isLoading() = calls.isNotEmpty()

    private fun cancelAndClean() {
        calls.forEach {
            it.cancel()
        }

        clean()
    }

    private fun clean() {
        calls.clear()
        result = null
        exception = null
    }

    private fun deliverAndCleanIfPossible() {
        if (result != null) {
            if (successCallback == null) {
                return
            } else {
                successCallback!!.invoke(result!!.data)
            }
        } else if (exception != null) {
            if (errorCallback == null) {
                return
            } else {
                errorCallback!!.invoke(exception!!)
            }
        } else {
            return
        }

        cancelAndClean()
    }

    class LoadingResult<out T>(val data: T) {

    }
}