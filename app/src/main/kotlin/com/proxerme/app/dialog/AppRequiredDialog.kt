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
import org.jetbrains.anko.bundleOf

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class AppRequiredDialog : DialogFragment() {

    companion object {
        private const val ARGUMENT_NAME = "name"
        private const val ARGUMENT_PACKAGE_NAME = "packageName"

        fun show(activity: AppCompatActivity, name: String, packageName: String) {
            AppRequiredDialog().apply {
                arguments = bundleOf(ARGUMENT_NAME to name, ARGUMENT_PACKAGE_NAME to packageName)
            }.show(activity.supportFragmentManager, "dialog_crunchyroll")
        }
    }

    private val name
        get() = arguments.getString(ARGUMENT_NAME)
    private val packageName
        get() = arguments.getString(ARGUMENT_PACKAGE_NAME)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        retainInstance = true
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialDialog.Builder(context)
                .title(getString(R.string.dialog_app_required_title, name))
                .content(getString(R.string.dialog_app_required_content, name))
                .positiveText(R.string.dialog_app_required_positive)
                .negativeText(R.string.dialog_cancel)
                .onPositive({ _, _ ->
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