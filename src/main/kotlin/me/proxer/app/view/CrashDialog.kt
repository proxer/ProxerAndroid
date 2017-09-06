package me.proxer.app.view

import android.app.Dialog
import android.content.ClipData
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import me.proxer.app.R
import me.proxer.app.base.BaseDialog
import me.proxer.app.util.extension.clipboardManager
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.toast

/**
 * @author Ruben Gees
 */
class CrashDialog : BaseDialog() {

    companion object {
        private const val ERROR_DETAILS_ARGUMENT = "error_details"
        private const val STACKTRACE_ARGUMENT = "stacktrace"

        fun show(activity: AppCompatActivity, errorDetails: String, stacktrace: String) = CrashDialog().apply {
            arguments = bundleOf(ERROR_DETAILS_ARGUMENT to errorDetails, STACKTRACE_ARGUMENT to stacktrace)
        }.show(activity.supportFragmentManager, "crash_dialog")
    }

    private val errorDetails: String
        get() = arguments.getString(ERROR_DETAILS_ARGUMENT)

    private val stacktrace: String
        get() = arguments.getString(STACKTRACE_ARGUMENT)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = MaterialDialog.Builder(context)
            .title(R.string.dialog_crash_title)
            .content("$errorDetails\n$stacktrace")
            .neutralText(R.string.dialog_crash_neutral)
            .negativeText(R.string.dialog_crash_negative)
            .onNeutral { _, _ ->
                context.clipboardManager.primaryClip = ClipData.newPlainText(getString(R.string.clipboard_crash_title),
                        "$errorDetails\n$stacktrace")

                context.toast(R.string.clipboard_status)
            }
            .build()
}
