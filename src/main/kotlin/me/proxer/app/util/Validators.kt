package me.proxer.app.util

import me.proxer.app.exception.AgeConfirmationRequiredException
import me.proxer.app.exception.NotLoggedInException
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper

/**
 * @author Ruben Gees
 */
class Validators(private val storageHelper: StorageHelper, private val preferenceHelper: PreferenceHelper) {

    fun validateLogin() {
        if (!storageHelper.isLoggedIn) {
            throw NotLoggedInException()
        }
    }

    fun validateAgeConfirmation() {
        if (!preferenceHelper.isAgeRestrictedMediaAllowed) {
            throw AgeConfirmationRequiredException()
        }
    }
}
