package com.proxerme.app.dialog

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.proxerme.app.R

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class CrunchyrollDialog : DialogFragment() {

    companion object {
        fun show(activity: AppCompatActivity) {
            CrunchyrollDialog().show(activity.supportFragmentManager, "dialog_crunchyroll")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        retainInstance = true
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialDialog.Builder(context)
                .title(R.string.dialog_crunchyroll_title)
                .content(R.string.dialog_crunchyroll_content)
                .positiveText(R.string.dialog_crunchyroll_positive)
                .negativeText(R.string.dialog_cancel)
                .onPositive({ _, _ ->
                    val packageName = "com.crunchyroll.crunchyroid"

                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=$packageName")))
                    } catch (e: ActivityNotFoundException) {
                        context.startActivity(Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
                    }
                })
                .build()
    }
}