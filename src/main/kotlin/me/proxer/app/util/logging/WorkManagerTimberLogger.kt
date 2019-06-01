package me.proxer.app.util.logging

import android.annotation.SuppressLint
import android.util.Log
import androidx.work.Logger
import timber.log.Timber

/**
 * @author Ruben Gees
 */
@SuppressLint("RestrictedApi")
class WorkManagerTimberLogger(loggingLevel: Int = Log.INFO) : Logger(loggingLevel) {

    override fun verbose(tag: String?, message: String, vararg throwables: Throwable) {
        log(Log.VERBOSE, message, throwables)
    }

    override fun debug(tag: String?, message: String, vararg throwables: Throwable) {
        log(Log.DEBUG, message, throwables)
    }

    override fun info(tag: String?, message: String, vararg throwables: Throwable) {
        log(Log.INFO, message, throwables)
    }

    override fun warning(tag: String?, message: String, vararg throwables: Throwable) {
        log(Log.WARN, message, throwables)
    }

    override fun error(tag: String?, message: String, vararg throwables: Throwable) {
        log(Log.ERROR, message, throwables)
    }

    private fun log(priority: Int, message: String, throwables: Array<out Throwable>) {
        if (throwables.isEmpty()) {
            Timber.log(priority, message)
        } else {
            throwables.forEach { Timber.log(priority, message, it) }
        }
    }
}
