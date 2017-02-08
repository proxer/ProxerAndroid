package com.proxerme.app.util

import android.content.Context
import com.proxerme.app.helper.PreferenceHelper
import com.proxerme.app.helper.StorageHelper

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
object Validators {

    fun validateLogin() {
        if (StorageHelper.user == null) {
            throw NotLoggedInException()
        }
    }

    fun validateHentaiConfirmation(context: Context) {
        if (!PreferenceHelper.isHentaiAllowed(context)) {
            throw HentaiConfirmationRequiredException()
        }
    }

    class NotLoggedInException : Exception()
    class HentaiConfirmationRequiredException : Exception()
}