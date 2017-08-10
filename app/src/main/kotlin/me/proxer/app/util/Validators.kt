package me.proxer.app.util

import android.content.Context
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

//    fun validateNewChatInput(context: Context, input: NewChatTaskInput) {
//        if (input.isGroup && input.topic.isBlank()) {
//            throw TopicEmptyException()
//        }
//
//        if (input.firstMessage.isBlank()) {
//            throw InvalidInputException(context.getString(R.string.error_missing_message))
//        }
//
//        if (input.participants.isEmpty()) {
//            throw InvalidInputException(context.getString(R.string.error_missing_participants))
//        }
//    }

    class NotLoggedInException : Exception()
    class AgeConfirmationRequiredException : Exception()
//    class TopicEmptyException : Exception()
//    class InvalidInputException(message: String) : Exception(message)
}
