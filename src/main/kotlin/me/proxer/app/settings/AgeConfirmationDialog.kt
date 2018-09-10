package me.proxer.app.settings

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import me.proxer.app.R
import me.proxer.app.base.BaseDialog

/**
 * @author Ruben Gees
 */
class AgeConfirmationDialog : BaseDialog() {

    companion object {
        fun show(activity: AppCompatActivity) = AgeConfirmationDialog()
            .show(activity.supportFragmentManager, "age_confirmation_dialog")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = MaterialDialog(requireContext())
        .message(R.string.dialog_age_confirmation_content)
        .positiveButton(R.string.dialog_age_confirmation_positive) {
            preferenceHelper.isAgeRestrictedMediaAllowed = true

            bus.post(AgeConfirmationEvent())
        }
        .negativeButton(R.string.cancel)
}
