package com.proxerme.app.util

import android.content.Context
import com.proxerme.app.helper.PreferenceHelper
import com.proxerme.app.helper.StorageHelper
import com.proxerme.app.stream.StreamResolverFactory
import com.proxerme.app.task.StreamResolutionTask

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

    fun validateResolverExists(name: String) {
        if (StreamResolverFactory.getResolverFor(name) == null) {
            throw StreamResolutionTask.NoResolverException()
        }
    }

    class NotLoggedInException : Exception()
    class HentaiConfirmationRequiredException : Exception()
}