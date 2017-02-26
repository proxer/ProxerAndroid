package com.proxerme.app.util

import android.view.View
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.proxerme.app.R
import com.proxerme.app.activity.MainActivity
import com.proxerme.app.dialog.HentaiConfirmationDialog
import com.proxerme.app.dialog.LoginDialog
import com.proxerme.app.event.CaptchaSolvedEvent
import com.proxerme.app.service.ChatService
import com.proxerme.app.task.StreamResolutionTask
import com.proxerme.app.util.ErrorUtils.ErrorAction.Companion.ACTION_MESSAGE_DEFAULT
import com.proxerme.library.connection.ProxerException
import com.proxerme.library.connection.ProxerException.*
import okhttp3.HttpUrl
import org.greenrobot.eventbus.EventBus
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * A helper class, turning error codes into human-readable Strings and producing appropriate actions
 * from given Exceptions.
 *
 * @author Ruben Gees
 */
object ErrorUtils {

    val API_ERRORS = arrayOf(UNKNOWN_API, API_REMOVED, UNKNOWN_API_CLASS, UNKNOWN_API_FUNCTION,
            FUNCTION_BLOCKED)
    val LOGIN_ERRORS = arrayOf(LOGIN_ALREADY_LOGGED_IN, LOGIN_DIFFERENT_USER_ALREADY_LOGGED_IN,
            LOGIN_MISSING_CREDENTIALS)
    val INVALID_USER_ERRORS = arrayOf(USERINFO_INVALID_ID, UCP_INVALID_ID)
    val INVALID_CATEGORY_ERRORS = arrayOf(UCP_INVALID_CATEGORY, LIST_UNKNOWN_CATEGORY)
    val INVALID_MEDIA_ERRORS = arrayOf(INFO_INVALID_ID, MEDIA_UNKNOWN_ENTRY)
    val NOT_LOGGED_IN_ERRORS = arrayOf(INVALID_TOKEN, INFO_USER_NOT_LOGGED_IN,
            NOTIFICATIONS_USER_NOT_LOGGED_IN, MESSAGES_USER_NOT_LOGGED_IN, UCP_USER_NOT_LOGGED_IN)

    fun getMessageForProxerException(exception: ProxerException): Int {
        return when (exception.errorCode) {
            PROXER -> {
                when (exception.proxerErrorCode) {
                    IP_BLOCKED -> R.string.error_ip_blocked
                    NEWS -> R.string.error_news
                    LOGIN_INVALID_CREDENTIALS -> R.string.error_login_invalid_credentials
                    INFO_INVALID_TYPE -> R.string.error_invalid_type
                    INFO_ENTRY_ALREADY_IN_LIST -> R.string.error_already_in_list
                    INFO_EXCEEDED_ALLOWED_ENTRIES -> R.string.error_favorites_full
                    USER_ACCESS_DENIED -> R.string.error_access_denied
                    LIST_UNKNOWN_MEDIUM -> R.string.error_invalid_medium
                    MEDIA_UNKNOWN_STYLE -> R.string.error_invalid_style
                    MANGA_UNKNOWN_CHAPTER -> R.string.error_manga_not_available
                    ANIME_UNKNOWN_EPISODE -> R.string.error_anime_not_available
                    ANIME_UNKNOWN_STREAM -> R.string.error_anime_stream_not_available
                    UCP_UNKNOWN_EPISODE -> R.string.error_episode_does_not_exist
                    MESSAGES_INVALID_CONFERENCE -> R.string.error_invalid_conference
                    MESSAGES_MISSING_REPORT_INPUT -> R.string.error_invalid_report_input
                    MESSAGES_INVALID_MESSAGE -> R.string.error_invalid_message
                    MESSAGES_INVALID_USER -> R.string.error_invalid_user
                    MESSAGES_MAXIMUM_USERS_EXCEEDED -> R.string.error_maximum_users_exceeded
                    MESSAGES_INVALID_TOPIC -> R.string.error_invalid_topic
                    MESSAGES_MISSING_USER -> R.string.error_missing_user
                    in API_ERRORS -> R.string.error_api
                    in LOGIN_ERRORS -> R.string.error_login
                    in INVALID_USER_ERRORS -> R.string.error_user_does_not_exist
                    in INVALID_CATEGORY_ERRORS -> R.string.error_invalid_category
                    in INVALID_MEDIA_ERRORS -> R.string.error_media_does_not_exist
                    in NOT_LOGGED_IN_ERRORS -> R.string.error_not_logged_in
                    else -> R.string.error_unknown
                }
            }
            TIMEOUT -> R.string.error_timeout
            NETWORK -> R.string.error_network
            UNPARSABLE -> R.string.error_unparseable
            else -> R.string.error_unknown
        }
    }

    fun handle(context: MainActivity, exception: Exception): ErrorAction {
        return when (exception) {
            is ProxerException -> {
                val message = getMessageForProxerException(exception)
                val buttonMessage = when (exception.proxerErrorCode) {
                    IP_BLOCKED -> R.string.error_action_captcha
                    in ErrorUtils.NOT_LOGGED_IN_ERRORS -> {
                        R.string.error_action_login
                    }
                    else -> ACTION_MESSAGE_DEFAULT
                }
                val buttonAction = when (exception.proxerErrorCode) {
                    IP_BLOCKED -> View.OnClickListener {
                        context.showPage(HttpUrl.Builder()
                                .scheme("https")
                                .host("proxer.me")
                                .addPathSegment("misc")
                                .addPathSegment("captcha")
                                .addQueryParameter("device", "mobile")
                                .build())

                        EventBus.getDefault().post(CaptchaSolvedEvent())
                    }
                    in ErrorUtils.NOT_LOGGED_IN_ERRORS -> View.OnClickListener {
                        LoginDialog.show(context)
                    }
                    else -> null
                }

                ErrorAction(message, buttonMessage, buttonAction)
            }
            is Validators.NotLoggedInException -> {
                val message = R.string.error_not_logged_in
                val buttonMessage = R.string.error_action_login
                val buttonAction = View.OnClickListener {
                    LoginDialog.show(context)
                }

                ErrorAction(message, buttonMessage, buttonAction)
            }
            is Validators.HentaiConfirmationRequiredException -> {
                ErrorAction(R.string.error_hentai_confirmation_needed, R.string.error_confirm,
                        View.OnClickListener {
                            HentaiConfirmationDialog.show(context)
                        })
            }
            is SocketTimeoutException -> {
                ErrorAction(R.string.error_timeout)
            }
            is IOException -> {
                ErrorAction(R.string.error_network)
            }
            is ChatService.ChatException -> {
                handle(context, exception.innerException)
            }
            is StreamResolutionTask.NoResolverException -> {
                ErrorAction(R.string.error_unsupported_hoster)
            }
            is StreamResolutionTask.StreamResolutionException -> {
                ErrorAction(R.string.error_stream_resolution)
            }
            is HttpDataSource.InvalidResponseCodeException -> {
                ErrorAction(when (exception.responseCode) {
                    404 -> R.string.error_video_deleted
                    in 400 until 600 -> R.string.error_video_unknown
                    else -> R.string.error_unknown
                })
            }
            else -> ErrorAction(R.string.error_unknown)
        }
    }

    class ErrorAction(val message: Int, val buttonMessage: Int = ACTION_MESSAGE_DEFAULT,
                      val buttonAction: View.OnClickListener? = null) {
        companion object {
            const val ACTION_MESSAGE_DEFAULT = -1
            const val ACTION_MESSAGE_HIDE = -2
        }
    }
}
