@file:Suppress("NOTHING_TO_INLINE")

package me.proxer.app.util.extension

import android.view.View
import com.google.android.material.snackbar.Snackbar.LENGTH_LONG
import me.proxer.app.base.BaseActivity
import me.proxer.app.util.ErrorUtils.ErrorAction.Companion.ACTION_MESSAGE_DEFAULT

inline fun BaseActivity.snackbar(
    message: Int,
    duration: Int = LENGTH_LONG,
    actionMessage: Int = ACTION_MESSAGE_DEFAULT,
    actionCallback: View.OnClickListener? = null
) = snackbar(getString(message), duration, actionMessage, actionCallback)

inline fun BaseActivity.multilineSnackbar(
    message: CharSequence,
    duration: Int = LENGTH_LONG,
    actionMessage: Int = ACTION_MESSAGE_DEFAULT,
    actionCallback: View.OnClickListener? = null,
    maxLines: Int = 5
) = snackbar(message, duration, actionMessage, actionCallback, maxLines)

inline fun BaseActivity.multilineSnackbar(
    message: Int,
    duration: Int = LENGTH_LONG,
    actionMessage: Int = ACTION_MESSAGE_DEFAULT,
    actionCallback: View.OnClickListener? = null,
    maxLines: Int = 5
) = snackbar(getString(message), duration, actionMessage, actionCallback, maxLines)
