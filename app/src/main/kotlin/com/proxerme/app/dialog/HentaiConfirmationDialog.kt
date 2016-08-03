package com.proxerme.app.dialog

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.proxerme.app.helper.PreferenceHelper

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class HentaiConfirmationDialog : DialogFragment() {

    companion object {
        fun show(activity: AppCompatActivity) {
            HentaiConfirmationDialog()
                    .show(activity.supportFragmentManager, "dialog_hentai_confirmation")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialDialog.Builder(context)
                .content("Dieser Bereich darf nur von Benutzern über 18 Jahren betreten werden! Eltern haften für ihre Kinder. Proxer.Me übernimmt keinerlei Haftung!")
                .positiveText("Verstanden")
                .negativeText("Abbrechen")
                .onPositive { materialDialog, dialogAction ->
                    PreferenceHelper.setHentaiAllowed(context, true)
                }
                .build()
    }
}