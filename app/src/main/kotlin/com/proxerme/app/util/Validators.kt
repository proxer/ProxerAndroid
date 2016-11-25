package com.proxerme.app.util

import android.content.Context
import com.proxerme.app.helper.PreferenceHelper
import com.proxerme.app.manager.UserManager

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
object Validators {

    fun validateLogin(isLoginRequired: Boolean) {
        if (isLoginRequired) {
            if (UserManager.loginState != UserManager.LoginState.LOGGED_IN ||
                    UserManager.ongoingState != UserManager.OngoingState.NONE) {
                throw NotLoggedInException()
            }
        }
    }

    fun validateHentaiConfirmation(context: Context, isHentaiConfirmationRequired: Boolean) {
        if (isHentaiConfirmationRequired) {
            if (!PreferenceHelper.isHentaiAllowed(context)) {
                throw HentaiConfirmationRequiredException()
            }
        }
    }

    class NotLoggedInException : Exception()
    class HentaiConfirmationRequiredException : Exception()
}