package com.proxerme.app.util

import android.view.View
import com.proxerme.app.R
import com.proxerme.app.activity.MainActivity
import com.proxerme.app.dialog.HentaiConfirmationDialog
import com.proxerme.app.dialog.LoginDialog
import com.proxerme.app.event.CaptchaSolvedEvent
import com.proxerme.app.service.ChatService
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

    val NOT_LOGGED_IN_ERRORS = arrayOf(INVALID_TOKEN, INFO_USER_NOT_LOGGED_IN,
            NOTIFICATIONS_USER_NOT_LOGGED_IN, MESSAGES_USER_NOT_LOGGED_IN, UCP_USER_NOT_LOGGED_IN)

    fun getMessageForErrorCode(exception: ProxerException): Int {
        when (exception.errorCode) {
            PROXER -> {
                return when (exception.proxerErrorCode) {
                    IP_BLOCKED -> R.string.error_ip_blocked
                    INFO_ENTRY_ALREADY_IN_LIST -> R.string.error_already_in_list
                    INFO_EXCEEDED_ALLOWED_ENTRIES -> R.string.error_favorites_full
                    in NOT_LOGGED_IN_ERRORS -> R.string.error_not_logged_in
                    else -> R.string.error_unknown
                }
            }
            TIMEOUT -> return R.string.error_timeout
            NETWORK -> return R.string.error_network
            UNPARSABLE -> return R.string.error_unparseable
            else -> return R.string.error_unknown
        }
    }

    fun handle(context: MainActivity, exception: Exception): ErrorAction {
        when (exception) {
            is ProxerException -> {
                val message = getMessageForErrorCode(exception)
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

                return ErrorAction(message, buttonMessage, buttonAction)
            }
            is Validators.NotLoggedInException -> {
                val message = R.string.error_not_logged_in
                val buttonMessage = R.string.error_action_login
                val buttonAction = View.OnClickListener {
                    LoginDialog.show(context)
                }

                return ErrorAction(message, buttonMessage, buttonAction)
            }
            is Validators.HentaiConfirmationRequiredException -> {
                return ErrorAction(R.string.error_hentai_confirmation_needed, R.string.error_confirm,
                        View.OnClickListener {
                            HentaiConfirmationDialog.show(context)
                        })
            }
            is SocketTimeoutException -> {
                return ErrorAction(R.string.error_timeout)
            }
            is IOException -> {
                return ErrorAction(R.string.error_network)
            }
            is ChatService.ChatException -> {
                return handle(context, exception.innerException)
            }
            else -> return ErrorAction(R.string.error_unknown)
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
