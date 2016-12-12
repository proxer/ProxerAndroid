package com.proxerme.app.util

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.klinker.android.link_builder.Link
import com.klinker.android.link_builder.TouchableMovementMethod
import com.proxerme.app.R
import com.proxerme.library.connection.ProxerException
import com.rubengees.easyheaderfooteradapter.EasyHeaderFooterAdapter
import org.jetbrains.anko.find

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
                  buttonMessage: String? = null, parent: ViewGroup? = null,
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
            text = buttonMessage ?: context.getString(R.string.error_retry)

            setOnClickListener(onButtonClickListener)
        }

        adapter.setFooter(errorContainer)
    }

}
