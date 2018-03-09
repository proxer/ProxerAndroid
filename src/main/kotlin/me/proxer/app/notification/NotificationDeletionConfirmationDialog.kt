package me.proxer.app.notification

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import me.proxer.app.R
import me.proxer.app.base.BaseDialog
import me.proxer.app.util.extension.unsafeLazy

/**
 * @author Ruben Gees
 */
class NotificationDeletionConfirmationDialog : BaseDialog() {

    companion object {
        fun show(activity: AppCompatActivity, fragment: Fragment) = NotificationDeletionConfirmationDialog().apply {
            setTargetFragment(fragment, 0)
        }.show(activity.supportFragmentManager, "notification_deletion_confirmation_dialog")
    }

    private val viewModel by unsafeLazy { NotificationViewModelProvider.get(requireTargetFragment()) }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = MaterialDialog.Builder(requireContext())
        .content(R.string.dialog_notification_deletion_confirmation_content)
        .positiveText(R.string.dialog_notification_deletion_confirmation_positive)
        .negativeText(R.string.cancel)
        .onPositive { _, _ -> viewModel.deleteAll() }
        .build()
}
