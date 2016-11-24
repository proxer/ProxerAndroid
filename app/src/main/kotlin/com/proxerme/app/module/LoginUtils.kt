package com.proxerme.app.module

import com.proxerme.app.manager.UserManager

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
object LoginUtils {

    fun loginValidator(isLoginRequired: Boolean) = {
        if (isLoginRequired) {
            if (UserManager.loginState != UserManager.LoginState.LOGGED_IN ||
                    UserManager.ongoingState != UserManager.OngoingState.NONE) {
                throw NotLoggedInException()
            }
        }
    }

    class NotLoggedInException : Exception()

}