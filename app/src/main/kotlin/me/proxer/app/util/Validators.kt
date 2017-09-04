package me.proxer.app.util

import android.content.Context
import me.proxer.app.exception.AgeConfirmationRequiredException
import me.proxer.app.exception.NotLoggedInException
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper

/**
 * @author Ruben Gees
 */
object Validators {

    fun validateLogin() {
        if (StorageHelper.user == null) {
            throw NotLoggedInException()
        }
    }

    fun validateAgeConfirmation(context: Context) {
        if (!PreferenceHelper.isAgeRestrictedMediaAllowed(context)) {
            throw AgeConfirmationRequiredException()
        }
    }
}
