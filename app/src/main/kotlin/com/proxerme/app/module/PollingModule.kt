package com.proxerme.app.module

import android.os.Handler

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
class PollingModule(private val callback: PollingModuleCallback, private val interval: Long) {

    private val handler = Handler()
    private val pollingRunnable = PollingRunnable()

    private var polling = false

    fun onResume() {
        startPolling()
    }

    fun onPause() {
        stopPolling()
    }

    fun onSuccessfulRequest() {
        startPolling()
    }

    fun onErrorRequest() {
        stopPolling()
    }

    private fun startPolling() {
        if (!polling) {
            polling = true

            handler.postDelayed(pollingRunnable, interval)
        }
    }

    private fun stopPolling() {
        polling = false

        handler.removeCallbacks(pollingRunnable)
    }

    interface PollingModuleCallback {
        val isLoading: Boolean
        val canLoad: Boolean

        fun load(showProgress: Boolean)
    }

    private inner class PollingRunnable : Runnable {
        override fun run() {
            if (callback.canLoad) {
                if (!callback.isLoading) {
                    callback.load(false)
                }

                handler.postDelayed(this, interval)
            } else {
                stopPolling()
            }
        }
    }

}