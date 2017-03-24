package me.proxer.app.util

import android.content.Context
import me.proxer.app.helper.PreferenceHelper
import me.proxer.app.helper.StorageHelper

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

//    fun validateResolverExists(name: String) {
//        if (StreamResolverFactory.getResolverFor(name) == null) {
//            throw StreamResolutionTask.NoResolverException()
//        }
//    }

    class NotLoggedInException : Exception()
    class HentaiConfirmationRequiredException : Exception()
}
