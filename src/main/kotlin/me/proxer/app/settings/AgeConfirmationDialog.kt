package me.proxer.app.settings

import android.app.Dialog
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import me.proxer.app.MainApplication.Companion.bus
import me.proxer.app.R
import me.proxer.app.base.BaseDialog
import me.proxer.app.util.data.PreferenceHelper

/**
 * @author Ruben Gees
 */
class AgeConfirmationDialog : BaseDialog() {

    companion object {
        fun show(activity: AppCompatActivity) = AgeConfirmationDialog()
                .show(activity.supportFragmentManager, "age_confirmation_dialog")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = MaterialDialog.Builder(context)
            .content(R.string.dialog_age_confirmation_content)
            .positiveText(R.string.dialog_age_confirmation_positive)
            .negativeText(R.string.cancel)
            .onPositive { _, _ ->
                PreferenceHelper.setAgeRestrictedMediaAllowed(context, true)

                bus.post(AgeConfirmationEvent())
            }
            .build()
}
