package me.proxer.app.dialog

import android.app.Dialog
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import me.proxer.app.R
import me.proxer.app.dialog.base.MainDialog
import me.proxer.app.event.HentaiConfirmationEvent
import me.proxer.app.helper.PreferenceHelper
import org.greenrobot.eventbus.EventBus

/**
 * @author Ruben Gees
 */
class HentaiConfirmationDialog : MainDialog() {

    companion object {
        fun show(activity: AppCompatActivity) {
            HentaiConfirmationDialog()
                    .show(activity.supportFragmentManager, "dialog_hentai_confirmation")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialDialog.Builder(context)
                .content(R.string.dialog_hentai_content)
                .positiveText(R.string.dialog_hentai_positive)
                .negativeText(R.string.dialog_cancel)
                .onPositive { _, _ ->
                    PreferenceHelper.setHentaiAllowed(context, true)

                    EventBus.getDefault().post(HentaiConfirmationEvent())
                }
                .build()
    }
}
