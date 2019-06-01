package me.proxer.app.util

import me.proxer.app.exception.AgeConfirmationRequiredException
import me.proxer.app.exception.NotLoggedInException
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.SecurePreferenceHelper

/**
 * @author Ruben Gees
 */
class Validators(
    private val securePreferenceHelper: SecurePreferenceHelper,
    private val preferenceHelper: PreferenceHelper
) {

    fun validateLogin() {
        if (!securePreferenceHelper.isLoggedIn) {
            throw NotLoggedInException()
        }
    }

    fun validateAgeConfirmation() {
        if (!preferenceHelper.isAgeRestrictedMediaAllowed) {
            throw AgeConfirmationRequiredException()
        }
    }
}
