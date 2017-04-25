package me.proxer.app.dialog

import android.app.Dialog
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import me.proxer.app.R
import me.proxer.app.dialog.base.MainDialog
import me.proxer.app.event.AgeConfirmationEvent
import me.proxer.app.helper.PreferenceHelper
import org.greenrobot.eventbus.EventBus

/**
 * @author Ruben Gees
 */
class AgeConfirmationDialog : MainDialog() {

    companion object {
        fun show(activity: AppCompatActivity) {
            AgeConfirmationDialog().show(activity.supportFragmentManager, "age_confirmation_dialog")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialDialog.Builder(context)
                .content(R.string.dialog_age_confirmation_content)
                .positiveText(R.string.dialog_age_confirmation_positive)
                .negativeText(R.string.cancel)
                .onPositive { _, _ ->
                    PreferenceHelper.setAgeRestrictedMediaAllowed(context, true)

                    EventBus.getDefault().post(AgeConfirmationEvent())
                }
                .build()
    }
}
