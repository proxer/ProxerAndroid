package me.proxer.app.util

import android.content.Context
import me.proxer.app.helper.PreferenceHelper
import me.proxer.app.helper.StorageHelper
import me.proxer.app.task.stream.StreamResolutionTask

/**
 * @author Ruben Gees
 */
object Validators {

    fun validateLogin() {
        if (StorageHelper.loginToken == null) {
            throw NotLoggedInException()
        }
    }

    fun validateAgeConfirmation(context: Context) {
        if (!PreferenceHelper.isAgeRestrictedMediaAllowed(context)) {
            throw HentaiConfirmationRequiredException()
        }
    }

    fun validateHosterSupported(name: String) {
        if (!StreamResolutionTask.isSupported(name)) {
            throw StreamResolutionTask.NoResolverException()
        }
    }

    class NotLoggedInException : Exception()
    class HentaiConfirmationRequiredException : Exception()
}
