package me.proxer.app.ui.crash

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import com.afollestad.materialdialogs.MaterialDialog
import me.proxer.app.R
import me.proxer.app.base.BaseDialog
import me.proxer.app.util.extension.getSafeString
import me.proxer.app.util.extension.toast

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

    @Suppress("DEPRECATION")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = MaterialDialog(requireContext())
        .title(R.string.dialog_crash_title)
        .message(text = errorDetails)
        .neutralButton(R.string.dialog_crash_neutral) {

            requireContext().getSystemService<ClipboardManager>()?.primaryClip =
                ClipData.newPlainText(getString(R.string.clipboard_crash_title), errorDetails)

            requireContext().toast(R.string.clipboard_status)
        }
        .negativeButton(R.string.dialog_crash_negative)
}
