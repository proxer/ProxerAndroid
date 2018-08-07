package me.proxer.app.ui.crash

import android.app.Dialog
import android.content.ClipData
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import me.proxer.app.R
import me.proxer.app.base.BaseDialog
import me.proxer.app.util.extension.clipboardManager
import me.proxer.app.util.extension.getSafeString
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.toast

/**
 * @author Ruben Gees
 */
class CrashDialog : BaseDialog() {

    companion object {
        private const val ERROR_DETAILS_ARGUMENT = "error_details"

        fun show(activity: AppCompatActivity, errorDetails: String) = CrashDialog().apply {
            arguments = bundleOf(ERROR_DETAILS_ARGUMENT to errorDetails)
        }.show(activity.supportFragmentManager, "crash_dialog")
    }

    private val errorDetails: String
        get() = requireArguments().getSafeString(ERROR_DETAILS_ARGUMENT)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = MaterialDialog.Builder(requireContext())
        .title(R.string.dialog_crash_title)
        .content(errorDetails)
        .neutralText(R.string.dialog_crash_neutral)
        .negativeText(R.string.dialog_crash_negative)
        .onNeutral { _, _ ->
            requireContext().clipboardManager.primaryClip = ClipData
                .newPlainText(getString(R.string.clipboard_crash_title), errorDetails)

            requireContext().toast(R.string.clipboard_status)
        }
        .build()
}
