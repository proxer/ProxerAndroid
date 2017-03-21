package me.proxer.app.fragment.base

import android.os.Bundle
import android.support.v4.app.Fragment

/**
 * @author Ruben Gees
 */
open class StateFragment<T> : Fragment() {

    open var data: T? = null
    open var error: Throwable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        retainInstance = true
    }

    fun clear() {
        data = null
        error = null
    }
}
