package me.proxer.app.util.logging

import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber

/**
 * @author Ruben Gees
 */
class HttpTimberLogger : HttpLoggingInterceptor.Logger {
    override fun log(message: String) {
        Timber.i(message)
    }
}
