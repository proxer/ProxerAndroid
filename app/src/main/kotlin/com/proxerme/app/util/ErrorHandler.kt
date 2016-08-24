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
            else -> return context.getString(R.string.error_unknown)
        }
    }

}
