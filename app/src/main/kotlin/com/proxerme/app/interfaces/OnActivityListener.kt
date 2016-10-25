package com.proxerme.app.interfaces

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
interface OnActivityListener {

    /**
     * Returns true if back press has been handled by this instance
     */
    fun onBackPressed(): Boolean {
        return true
    }
}