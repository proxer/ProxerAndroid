package me.proxer.app.ui.view

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import me.proxer.app.BuildConfig.APPLICATION_ID
import me.proxer.app.R
import me.proxer.app.base.BaseDialog
import me.proxer.app.util.data.PreferenceHelper

/**
 * @author Ruben Gees
 */
class RatingDialog : BaseDialog() {

    companion object {
        fun show(activity: AppCompatActivity) = RatingDialog()
            .show(activity.supportFragmentManager, "rating_dialog")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = MaterialDialog.Builder(requireContext())
        .title(getString(R.string.dialog_rating_title))
        .content(getString(R.string.dialog_rating_content))
        .positiveText(getString(R.string.dialog_rating_positive))
        .neutralText(getString(R.string.dialog_rating_neutral))
        .negativeText(getString(R.string.dialog_rating_negative))
        .onPositive { _, _ ->
            PreferenceHelper.setHasRated(requireContext())

            try {
                requireContext().startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=$APPLICATION_ID")))
            } catch (error: ActivityNotFoundException) {
                requireContext().startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$APPLICATION_ID")))
            }
        }
        .onNegative { _, _ -> PreferenceHelper.setHasRated(requireContext()) }
        .build()
}
