package me.proxer.app.notification

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.afollestad.materialdialogs.MaterialDialog
import me.proxer.app.R
import me.proxer.app.base.BaseDialog

/**
 * @author Ruben Gees
 */
class NotificationDeletionConfirmationDialog : BaseDialog() {

    companion object {
        const val DELETE_ALL_RESULT = "delete_all"

        fun show(activity: AppCompatActivity) = NotificationDeletionConfirmationDialog()
            .show(activity.supportFragmentManager, "notification_deletion_confirmation_dialog")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = MaterialDialog(requireContext())
        .message(R.string.dialog_notification_deletion_confirmation_content)
        .positiveButton(R.string.dialog_notification_deletion_confirmation_positive) {
            setFragmentResult(DELETE_ALL_RESULT, bundleOf())
        }
        .negativeButton(R.string.cancel)
}
