@file:Suppress("NOTHING_TO_INLINE")

package me.proxer.app.util.extension

import android.app.Activity
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.LENGTH_LONG
import me.proxer.app.R
import me.proxer.app.util.ErrorUtils.ErrorAction.Companion.ACTION_MESSAGE_DEFAULT
import me.proxer.app.util.ErrorUtils.ErrorAction.Companion.ACTION_MESSAGE_HIDE
import org.jetbrains.anko.applyRecursively

@Suppress("unused")
inline fun Activity.snackbar(
    root: View,
    message: CharSequence,
    duration: Int = LENGTH_LONG,
    actionMessage: Int = ACTION_MESSAGE_DEFAULT,
    actionCallback: View.OnClickListener? = null
) = Snackbar.make(root, message, duration).apply {
    when (actionMessage) {
        ACTION_MESSAGE_DEFAULT -> setAction(R.string.error_action_retry, actionCallback)
        ACTION_MESSAGE_HIDE -> setAction(null, null)
        else -> setAction(actionMessage, actionCallback)
    }

    show()
}

inline fun Activity.snackbar(
    root: View,
    message: Int,
    duration: Int = LENGTH_LONG,
    actionMessage: Int = ACTION_MESSAGE_DEFAULT,
    actionCallback: View.OnClickListener? = null
) = snackbar(root, getString(message), duration, actionMessage, actionCallback)

inline fun Activity.multilineSnackbar(
    root: View,
    message: CharSequence,
    duration: Int = LENGTH_LONG,
    actionMessage: Int = ACTION_MESSAGE_DEFAULT,
    actionCallback: View.OnClickListener? = null,
    maxLines: Int = 5
) = snackbar(root, message, duration, actionMessage, actionCallback).apply {
    view.applyRecursively {
        if (it is TextView && it !is Button) {
            it.maxLines = maxLines
        }
    }
}

inline fun Activity.multilineSnackbar(
    root: View,
    message: Int,
    duration: Int = LENGTH_LONG,
    actionMessage: Int = ACTION_MESSAGE_DEFAULT,
    actionCallback: View.OnClickListener? = null,
    maxLines: Int = 5
) = multilineSnackbar(root, getString(message), duration, actionMessage, actionCallback, maxLines)

inline fun Fragment.snackbar(
    root: View,
    message: CharSequence,
    duration: Int = LENGTH_LONG,
    actionMessage: Int = ACTION_MESSAGE_DEFAULT,
    actionCallback: View.OnClickListener? = null
) = activity?.snackbar(root, message, duration, actionMessage, actionCallback)

inline fun Fragment.snackbar(
    root: View,
    message: Int,
    duration: Int = LENGTH_LONG,
    actionMessage: Int = ACTION_MESSAGE_DEFAULT,
    actionCallback: View.OnClickListener? = null
) = activity?.snackbar(root, message, duration, actionMessage, actionCallback)

inline fun Fragment.multilineSnackbar(
    root: View,
    message: CharSequence,
    duration: Int = LENGTH_LONG,
    actionMessage: Int = ACTION_MESSAGE_DEFAULT,
    actionCallback: View.OnClickListener? = null,
    maxLines: Int = 5
) = activity?.multilineSnackbar(root, message, duration, actionMessage, actionCallback, maxLines)

inline fun Fragment.multilineSnackbar(
    root: View,
    message: Int,
    duration: Int = LENGTH_LONG,
    actionMessage: Int = ACTION_MESSAGE_DEFAULT,
    actionCallback: View.OnClickListener? = null,
    maxLines: Int = 5
) = activity?.multilineSnackbar(root, message, duration, actionMessage, actionCallback, maxLines)
