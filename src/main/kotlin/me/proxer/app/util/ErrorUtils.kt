package me.proxer.app.util

import android.view.View
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.upstream.HttpDataSource
import me.proxer.app.R
import me.proxer.app.auth.LoginDialog
import me.proxer.app.base.BaseActivity
import me.proxer.app.exception.*
import me.proxer.app.settings.AgeConfirmationDialog
import me.proxer.app.util.ErrorUtils.ErrorAction.ButtonAction
import me.proxer.app.util.ErrorUtils.ErrorAction.Companion.ACTION_MESSAGE_DEFAULT
import me.proxer.app.util.data.StorageHelper
import me.proxer.library.api.ProxerException
import me.proxer.library.api.ProxerException.ErrorType.*
import me.proxer.library.api.ProxerException.ServerErrorType.*
import me.proxer.library.enums.Device
import me.proxer.library.util.ProxerUrls
import java.io.IOException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLPeerUnverifiedException

/**
 * @author Ruben Gees
 */
object ErrorUtils {

    private val API_ERRORS = arrayOf(UNKNOWN_API, API_REMOVED, INVALID_API_CLASS, INVALID_API_FUNCTION,
            INSUFFICIENT_PERMISSIONS, FUNCTION_BLOCKED)
    private val MAINTENANCE_ERRORS = arrayOf(SERVER_MAINTENANCE, API_MAINTENANCE)
    private val LOGIN_ERRORS = arrayOf(INVALID_TOKEN, NOTIFICATIONS_LOGIN_REQUIRED, UCP_LOGIN_REQUIRED,
            INFO_LOGIN_REQUIRED, MESSAGES_LOGIN_REQUIRED, USER_2FA_SECRET_REQUIRED)
    private val CLIENT_ERRORS = arrayOf(NEWS, LOGIN_MISSING_CREDENTIALS, UCP_INVALID_CATEGORY, INFO_INVALID_TYPE,
            LOGIN_ALREADY_LOGGED_IN, LOGIN_DIFFERENT_USER_ALREADY_LOGGED_IN, LIST_INVALID_CATEGORY, LIST_INVALID_MEDIUM,
            MEDIA_INVALID_STYLE, ANIME_INVALID_STREAM, LIST_INVALID_LANGUAGE, LIST_INVALID_TYPE, ERRORLOG_INVALID_INPUT,
            LIST_INVALID_SUBJECT)
    private val INVALID_ID_ERRORS = arrayOf(USERINFO_INVALID_ID, UCP_INVALID_ID, INFO_INVALID_ID, MEDIA_INVALID_ENTRY,
            UCP_INVALID_EPISODE, MESSAGES_INVALID_CONFERENCE, LIST_INVALID_ID, FORUM_INVALID_ID, APPS_INVALID_ID)
    private val UNSUPPORTED_ERRORS = arrayOf(CHAT_INVALID_ROOM, CHAT_INVALID_PERMISSIONS, CHAT_INVALID_MESSAGE,
            CHAT_LOGIN_REQUIRED, USER)

    fun getMessage(error: Throwable): Int {
        val innermostError = getInnermostError(error)

        return when (innermostError) {
            is ProxerException -> getMessageForProxerException(innermostError)
            is SocketTimeoutException -> R.string.error_timeout
            is SSLPeerUnverifiedException -> R.string.error_ssl
            is IOException -> R.string.error_io
            is NotLoggedInException -> R.string.error_login_required
            is AgeConfirmationRequiredException -> R.string.error_age_confirmation_needed
            is StreamResolutionException -> R.string.error_stream_resolution
            is HttpDataSource.InvalidResponseCodeException -> when (innermostError.responseCode) {
                404 -> R.string.error_video_deleted
                in 400 until 600 -> R.string.error_video_unknown
                else -> R.string.error_unknown
            }
            else -> R.string.error_unknown
        }
    }

    fun isIpBlockedError(error: Throwable) = getInnermostError(error).let {
        it is ProxerException && it.serverErrorType == IP_BLOCKED
    }

    fun isNetworkError(error: Throwable) = ErrorUtils.getInnermostError(error).let {
        it is ProxerException && (it.errorType == IO || it.errorType == TIMEOUT)
    }

    fun handle(error: Throwable): ErrorAction {
        val innermostError = getInnermostError(error)
        val errorMessage = getMessage(innermostError)

        val buttonMessage = when (innermostError) {
            is ProxerException -> when (innermostError.serverErrorType) {
                IP_BLOCKED -> R.string.error_action_captcha
                in LOGIN_ERRORS -> R.string.error_action_login
                else -> ACTION_MESSAGE_DEFAULT
            }
            is NotLoggedInException -> R.string.error_action_login
            is AgeConfirmationRequiredException -> R.string.error_action_confirm
            else -> ACTION_MESSAGE_DEFAULT
        }

        val buttonAction = when (innermostError) {
            is ProxerException -> when (innermostError.serverErrorType) {
                IP_BLOCKED -> ButtonAction.CAPTCHA
                in LOGIN_ERRORS -> ButtonAction.LOGIN
                else -> null
            }
            is NotLoggedInException -> ButtonAction.LOGIN
            is AgeConfirmationRequiredException -> ButtonAction.AGE_CONFIRMATION
            else -> null
        }

        return ErrorAction(errorMessage, buttonMessage, buttonAction, (error as? PartialException)?.partialData)
    }

    private fun getMessageForProxerException(error: ProxerException) = when (error.errorType) {
        SERVER -> when (error.serverErrorType) {
            IP_BLOCKED -> R.string.error_captcha
            INVALID_TOKEN -> R.string.error_invalid_token
            LOGIN_INVALID_CREDENTIALS -> R.string.error_login_credentials
            INFO_ENTRY_ALREADY_IN_LIST -> R.string.error_already_in_list
            INFO_EXCEEDED_MAXIMUM_ENTRIES -> R.string.error_list_full
            MANGA_INVALID_CHAPTER -> R.string.error_invalid_chapter
            ANIME_INVALID_EPISODE -> R.string.error_invalid_episode
            MESSAGES_INVALID_REPORT_INPUT -> R.string.error_invalid_input
            MESSAGES_INVALID_MESSAGE -> R.string.error_invalid_input
            MESSAGES_INVALID_USER -> R.string.error_cant_add_user_to_conference
            MESSAGES_MISSING_USER -> R.string.error_invalid_users_for_conference
            MESSAGES_EXCEEDED_MAXIMUM_USERS -> R.string.error_conference_full
            MESSAGES_INVALID_TOPIC -> R.string.error_invalid_topic
            LIST_TOP_ACCESS_RESET -> R.string.error_list_top_access_reset
            USER_2FA_SECRET_REQUIRED -> R.string.error_login_two_factor_authentication
            USER_ACCOUNT_EXPIRED -> R.string.error_account_expired
            USER_ACCOUNT_BLOCKED -> R.string.error_account_blocked
            USER_INSUFFICIENT_PERMISSIONS -> when (!StorageHelper.isLoggedIn) {
                true -> R.string.error_insufficient_permissions
                false -> R.string.error_insufficient_permissions_logged_in
            }
            in API_ERRORS -> R.string.error_api
            in MAINTENANCE_ERRORS -> R.string.error_maintenance
            in LOGIN_ERRORS -> R.string.error_login
            in CLIENT_ERRORS -> R.string.error_client
            in INVALID_ID_ERRORS -> R.string.error_invalid_id
            in UNSUPPORTED_ERRORS -> R.string.error_unsupported_code
            else -> R.string.error_unknown
        }
        IO -> when (error.cause) {
            is SSLPeerUnverifiedException -> R.string.error_ssl
            else -> R.string.error_io
        }
        TIMEOUT -> R.string.error_timeout
        PARSING -> R.string.error_parsing
        CANCELLED -> R.string.error_unknown
        UNKNOWN -> R.string.error_unknown
    }

    private fun getInnermostError(error: Throwable) = when (error) {
        is PartialException -> error.innerError
        is ChatException -> error.innerError
        is ExoPlaybackException -> error.cause ?: Exception()
        else -> error
    }

    open class ErrorAction(
            val message: Int,
            val buttonMessage: Int = ACTION_MESSAGE_DEFAULT,
            val buttonAction: ButtonAction? = null,
            val partialData: Any? = null
    ) {

        companion object {
            const val ACTION_MESSAGE_DEFAULT = -1
            const val ACTION_MESSAGE_HIDE = -2
        }

        enum class ButtonAction {
            CAPTCHA, LOGIN, AGE_CONFIRMATION;

            fun toClickListener(activity: BaseActivity) = when (this) {
                CAPTCHA -> View.OnClickListener { activity.showPage(ProxerUrls.captchaWeb(Device.MOBILE)) }
                LOGIN -> View.OnClickListener { LoginDialog.show(activity) }
                AGE_CONFIRMATION -> View.OnClickListener { AgeConfirmationDialog.show(activity) }
            }
        }
    }
}
