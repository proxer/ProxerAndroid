package com.proxerme.app.util

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.klinker.android.link_builder.Link
import com.klinker.android.link_builder.TouchableMovementMethod
import com.proxerme.app.R
import com.proxerme.app.dialog.HentaiConfirmationDialog
import com.proxerme.app.dialog.LoginDialog
import com.proxerme.app.manager.UserManager
import com.proxerme.app.service.ChatService
import com.proxerme.library.connection.ProxerException
import com.rubengees.easyheaderfooteradapter.EasyHeaderFooterAdapter
import org.jetbrains.anko.find
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * A helper class, turning error codes into human-readable Strings.

 * @author Ruben Gees
 */
object ErrorUtils {

    fun getMessageForErrorCode(context: Context,
                               exception: ProxerException): String {
        when (exception.errorCode) {
            ProxerException.PROXER -> {
                return exception.message ?: context.getString(R.string.error_unknown)
            }
            ProxerException.TIMEOUT -> return context.getString(R.string.error_timeout)
            ProxerException.NETWORK -> return context.getString(R.string.error_network)
            ProxerException.UNPARSABLE -> return context.getString(R.string.error_unparseable)
            else -> return context.getString(R.string.error_unknown)
        }
    }

    fun showError(context: Context, message: CharSequence, adapter: EasyHeaderFooterAdapter,
                  buttonMessage: String? = "", parent: ViewGroup? = null,
                  onWebClickListener: Link.OnClickListener? = null,
                  onButtonClickListener: View.OnClickListener? = null) {
        val errorContainer = when {
            adapter.innerAdapter.itemCount <= 0 -> {
                LayoutInflater.from(context).inflate(R.layout.layout_error, parent, false)
            }
            else -> LayoutInflater.from(context).inflate(R.layout.item_error, parent, false)
        }

        errorContainer.find<TextView>(R.id.errorText).apply {
            movementMethod = TouchableMovementMethod.getInstance()
            text = Utils.buildClickableText(context, message, onWebClickListener)
        }

        errorContainer.find<Button>(R.id.errorButton).apply {
            when (buttonMessage) {
                null -> visibility = View.GONE
                else -> {
                    visibility = View.VISIBLE
                    setOnClickListener(onButtonClickListener)

                    when {
                        buttonMessage.isBlank() -> text = context.getString(R.string.error_retry)
                        else -> text = buttonMessage
                    }
                }
            }
        }

        adapter.setFooter(errorContainer)
    }

    fun handle(context: AppCompatActivity, exception: Exception): ErrorAction {
        when (exception) {
            is ProxerException -> {
                return ErrorAction(ErrorUtils.getMessageForErrorCode(context, exception))
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
                    UserManager.OngoingState.NONE -> context.getString(R.string.module_login_login)
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

    class ErrorAction(val message: String, val buttonMessage: String? = null,
                      val buttonAction: View.OnClickListener? = null)
}
