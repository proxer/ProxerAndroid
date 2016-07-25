package com.proxerme.app.util

import android.content.Context
import com.proxerme.app.R
import com.proxerme.library.connection.ProxerException

/**
 * A helper class, turning error codes into human-readable Strings.

 * @author Ruben Gees
 */
object ErrorHandler {

    fun getMessageForErrorCode(context: Context,
                               exception: ProxerException): String {
        when (exception.errorCode) {
            ProxerException.PROXER -> return exception.message!!
            ProxerException.NETWORK -> return context.getString(R.string.error_network)
            ProxerException.TIMEOUT -> return context.getString(R.string.error_timeout)
            ProxerException.UNPARSEABLE -> return context.getString(R.string.error_unparseable)
            ProxerException.IO -> return context.getString(R.string.error_io)
            ProxerException.UNKNOWN -> return context.getString(R.string.error_unknown)
            else -> return context.getString(R.string.error_unknown)
        }
    }

}
