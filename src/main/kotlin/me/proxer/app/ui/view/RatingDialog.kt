package me.proxer.app.ui.view

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import me.proxer.app.BuildConfig.APPLICATION_ID
import me.proxer.app.R
import me.proxer.app.base.BaseDialog

/**
 * @author Ruben Gees
 */
class RatingDialog : BaseDialog() {

    companion object {
        fun show(activity: AppCompatActivity) = RatingDialog()
            .show(activity.supportFragmentManager, "rating_dialog")
    }

    @Suppress("DEPRECATION")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = MaterialDialog(requireContext())
        .title(R.string.dialog_rating_title)
        .message(R.string.dialog_rating_content)
        .positiveButton(R.string.dialog_rating_positive) {
            preferenceHelper.hasRated = true

            try {
                requireContext().startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=$APPLICATION_ID")
                    )
                )
            } catch (error: ActivityNotFoundException) {
                requireContext().startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=$APPLICATION_ID")
                    )
                )
            }
        }
        .neutralButton(R.string.dialog_rating_neutral)
        .negativeButton(R.string.dialog_rating_negative) {
            preferenceHelper.hasRated = true
        }
}
