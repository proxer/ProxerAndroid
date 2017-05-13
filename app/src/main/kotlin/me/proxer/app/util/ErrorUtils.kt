package me.proxer.app.util

import android.view.View
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.rubengees.ktask.base.MultiBranchTask.FullTaskException
import com.rubengees.ktask.base.MultiBranchTask.PartialTaskException
import me.proxer.app.R
import me.proxer.app.activity.base.MainActivity
import me.proxer.app.dialog.AgeConfirmationDialog
import me.proxer.app.dialog.LoginDialog
import me.proxer.app.task.stream.StreamResolutionTask
import me.proxer.app.util.ErrorUtils.ErrorAction.Companion.ACTION_MESSAGE_DEFAULT
import me.proxer.app.util.Validators.HentaiConfirmationRequiredException
import me.proxer.app.util.Validators.NotLoggedInException
import me.proxer.library.api.ProxerException
import me.proxer.library.api.ProxerException.ErrorType.*
import me.proxer.library.api.ProxerException.ServerErrorType.*
import me.proxer.library.enums.Device
import me.proxer.library.util.ProxerUrls
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * @author Ruben Gees
 */
object ErrorUtils {

    val API_ERRORS = arrayOf(UNKNOWN_API, API_REMOVED, INVALID_API_CLASS, INVALID_API_FUNCTION,
            INSUFFICIENT_PERMISSIONS, FUNCTION_BLOCKED)
    val MAINTENANCE_ERRORS = arrayOf(SERVER_MAINTENANCE, API_MAINTENANCE)
    val LOGIN_ERRORS = arrayOf(INVALID_TOKEN, NOTIFICATIONS_LOGIN_REQUIRED, UCP_LOGIN_REQUIRED,
            INFO_LOGIN_REQUIRED, MESSAGES_LOGIN_REQUIRED, USER_2FA_SECRET_REQUIRED)
    val CLIENT_ERRORS = arrayOf(NEWS, LOGIN_MISSING_CREDENTIALS, UCP_INVALID_CATEGORY, INFO_INVALID_TYPE,
            LOGIN_ALREADY_LOGGED_IN, LOGIN_DIFFERENT_USER_ALREADY_LOGGED_IN, LIST_INVALID_CATEGORY, LIST_INVALID_MEDIUM,
            MEDIA_INVALID_STYLE, ANIME_INVALID_STREAM, LIST_INVALID_LANGUAGE, LIST_INVALID_TYPE, ERRORLOG_INVALID_INPUT,
            MESSAGES_MISSING_USER)
    val INVALID_ID_ERRORS = arrayOf(USERINFO_INVALID_ID, UCP_INVALID_ID, INFO_INVALID_ID, MEDIA_INVALID_ENTRY,
            UCP_INVALID_EPISODE, MESSAGES_INVALID_CONFERENCE, LIST_INVALID_ID)
    val UNSUPPORTED_ERRORS = arrayOf(CHAT_INVALID_ROOM, CHAT_INVALID_PERMISSIONS, CHAT_INVALID_MESSAGE,
            CHAT_LOGIN_REQUIRED, USER)

    fun getMessage(error: Throwable): Int {
        val innermostError = getInnermostError(error)

        return when (innermostError) {
            is ProxerException -> {
                getMessageForProxerException(innermostError)
            }
            is SocketTimeoutException -> {
                R.string.error_timeout
            }
            is HttpDataSource.InvalidResponseCodeException -> {
                when (innermostError.responseCode) {
                    404 -> R.string.error_video_deleted
                    in 400 until 600 -> R.string.error_video_unknown
                    else -> R.string.error_unknown
                }
            }
            is IOException -> {
                R.string.error_io
            }
            is NotLoggedInException -> {
                R.string.error_login_required
            }
            is HentaiConfirmationRequiredException -> {
                R.string.error_age_confirmation_needed
            }
            is StreamResolutionTask.NoResolverException -> {
                R.string.error_unsupported_hoster
            }
            is StreamResolutionTask.StreamResolutionException -> {
                R.string.error_stream_resolution
            }
            else -> R.string.error_unknown
        }
    }

    fun getMessageForProxerException(error: ProxerException): Int {
        return when (error.errorType) {
            SERVER -> {
                when (error.serverErrorType) {
                    IP_BLOCKED -> R.string.error_captcha
                    INVALID_TOKEN -> R.string.error_invalid_token
                    LOGIN_INVALID_CREDENTIALS -> R.string.error_login_credentials
                    INFO_ENTRY_ALREADY_IN_LIST -> R.string.error_already_in_list
                    INFO_EXCEEDED_MAXIMUM_ENTRIES -> R.string.error_list_full
                    USER_INSUFFICIENT_PERMISSIONS -> R.string.error_insufficient_permissions
                    MANGA_INVALID_CHAPTER -> R.string.error_invalid_chapter
                    ANIME_INVALID_EPISODE -> R.string.error_invalid_episode
                    MESSAGES_INVALID_REPORT_INPUT -> R.string.error_invalid_input
                    MESSAGES_INVALID_MESSAGE -> R.string.error_invalid_input
                    MESSAGES_INVALID_USER -> R.string.error_cant_add_user_to_conference
                    MESSAGES_EXCEEDED_MAXIMUM_USERS -> R.string.error_conference_full
                    MESSAGES_INVALID_TOPIC -> R.string.error_invalid_topic
                    USER_ACCOUNT_EXPIRED -> R.string.error_account_expired
                    USER_ACCOUNT_BLOCKED -> R.string.error_account_blocked
                    in API_ERRORS -> R.string.error_api
                    in MAINTENANCE_ERRORS -> R.string.error_maintenance
                    in LOGIN_ERRORS -> R.string.error_login
                    in CLIENT_ERRORS -> R.string.error_client
                    in INVALID_ID_ERRORS -> R.string.error_invalid_id
                    in UNSUPPORTED_ERRORS -> R.string.error_unsupported_code
                    else -> R.string.error_unknown
                }
            }
            TIMEOUT -> R.string.error_timeout
            IO -> R.string.error_io
            PARSING -> R.string.error_parsing
            UNKNOWN -> R.string.error_unknown
        }
    }

    fun getInnermostError(error: Throwable): Throwable {
        return when (error) {
            is FullTaskException -> error.firstInnerError
            is PartialTaskException -> error.innerError
//            is ChatService.ChatException -> error.innerError
            is ExoPlaybackException -> error.cause ?: Exception()
            else -> error
        }
    }

    fun handle(context: MainActivity, error: Throwable): ErrorAction {
        val innermostError = getInnermostError(error)
        val errorMessage = getMessage(innermostError)

        val buttonMessage = when (innermostError) {
            is ProxerException -> when (innermostError.serverErrorType) {
                IP_BLOCKED -> R.string.error_action_captcha
                in LOGIN_ERRORS -> R.string.error_action_login
                else -> ACTION_MESSAGE_DEFAULT
            }
            is NotLoggedInException -> R.string.error_action_login
            is HentaiConfirmationRequiredException -> R.string.error_action_confirm
            else -> ACTION_MESSAGE_DEFAULT
        }

        val buttonAction = when (innermostError) {
            is ProxerException -> when (innermostError.serverErrorType) {
                IP_BLOCKED -> View.OnClickListener {
                    context.showPage(ProxerUrls.captchaWeb(Device.MOBILE))
                }
                in LOGIN_ERRORS -> View.OnClickListener {
                    LoginDialog.show(context)
                }
                else -> null
            }
            is NotLoggedInException -> View.OnClickListener {
                LoginDialog.show(context)
            }
            is HentaiConfirmationRequiredException -> View.OnClickListener {
                AgeConfirmationDialog.show(context)
            }
            else -> null
        }

        return ErrorAction(errorMessage, buttonMessage, buttonAction)
    }

    class ErrorAction(val message: Int, val buttonMessage: Int = ACTION_MESSAGE_DEFAULT,
                      val buttonAction: View.OnClickListener? = null) {
        companion object {
            const val ACTION_MESSAGE_DEFAULT = -1
            const val ACTION_MESSAGE_HIDE = -2
        }
    }
}
