package com.proxerme.app.util

import android.content.Context
import android.view.View
import com.proxerme.app.R
import com.proxerme.app.activity.MainActivity
import com.proxerme.app.dialog.HentaiConfirmationDialog
import com.proxerme.app.dialog.LoginDialog
import com.proxerme.app.event.CaptchaSolvedEvent
import com.proxerme.app.manager.UserManager
import com.proxerme.app.service.ChatService
import com.proxerme.library.connection.ProxerException
import com.proxerme.library.connection.ProxerException.IP_BLOCKED
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

    fun getMessageForErrorCode(context: Context,
                               exception: ProxerException): String {
        when (exception.errorCode) {
            ProxerException.PROXER -> {
                return when (exception.proxerErrorCode) {
                    IP_BLOCKED -> context.getString(R.string.error_ip_blocked)
                    else -> exception.message ?: context.getString(R.string.error_unknown)
                }
            }
            ProxerException.TIMEOUT -> return context.getString(R.string.error_timeout)
            ProxerException.NETWORK -> return context.getString(R.string.error_network)
            ProxerException.UNPARSABLE -> return context.getString(R.string.error_unparseable)
            else -> return context.getString(R.string.error_unknown)
        }
    }

    fun handle(context: MainActivity, exception: Exception): ErrorAction {
        when (exception) {
            is ProxerException -> {
                val message = getMessageForErrorCode(context, exception)
                val buttonMessage = when (exception.proxerErrorCode) {
                    ProxerException.IP_BLOCKED -> context.getString(R.string.error_action_captcha)
                    else -> ""
                }
                val buttonAction = when (exception.proxerErrorCode) {
                    ProxerException.IP_BLOCKED -> View.OnClickListener {
                        context.showPage(HttpUrl.Builder()
                                .scheme("https")
                                .host("proxer.me")
                                .addPathSegment("misc")
                                .addPathSegment("captcha")
                                .addQueryParameter("device", "mobile")
                                .build())

                        EventBus.getDefault().post(CaptchaSolvedEvent())
                    }
                    else -> null
                }

                return ErrorAction(message, buttonMessage, buttonAction)
            }
            is Validators.NotLoggedInException -> {
                val message = when (UserManager.ongoingState) {
                    UserManager.OngoingState.LOGGING_IN -> {
                        context.getString(R.string.status_currently_logging_in)
                    }
                    UserManager.OngoingState.LOGGING_OUT -> {
                        context.getString(R.string.status_currently_logging_out)
                    }
                    else -> context.getString(R.string.status_not_logged_in)
                }

                val buttonMessage = when (UserManager.ongoingState) {
                    UserManager.OngoingState.NONE -> context.getString(R.string.error_action_login)
                    else -> null
                }

                val buttonAction = when (UserManager.ongoingState) {
                    UserManager.OngoingState.NONE -> View.OnClickListener {
                        LoginDialog.show(context)
                    }
                    else -> null
                }

                return ErrorAction(message, buttonMessage, buttonAction)
            }
            is Validators.HentaiConfirmationRequiredException -> {
                return ErrorAction(context.getString(R.string.error_hentai_confirmation_needed),
                        context.getString(R.string.error_confirm), View.OnClickListener {
                    HentaiConfirmationDialog.show(context)
                })
            }
            is SocketTimeoutException -> {
                return ErrorAction(context.getString(R.string.error_timeout))
            }
            is IOException -> {
                return ErrorAction(context.getString(R.string.error_network))
            }
            is ChatService.ChatException -> {
                return ErrorAction(exception.message ?: context.getString(R.string.error_unknown))
            }
            else -> return ErrorAction(context.getString(R.string.error_unknown))
        }
    }

    class ErrorAction(val message: String, val buttonMessage: String? = "",
                      val buttonAction: View.OnClickListener? = null)
}
