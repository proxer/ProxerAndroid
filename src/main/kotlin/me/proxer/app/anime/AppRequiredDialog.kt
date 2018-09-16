package me.proxer.app.anime

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import com.afollestad.materialdialogs.MaterialDialog
import me.proxer.app.R
import me.proxer.app.base.BaseDialog

/**
 * @author Ruben Gees
 */
class AppRequiredDialog : BaseDialog() {

    companion object {
        private const val NAME_ARGUMENT = "name"
        private const val PACKAGE_NAME_ARGUMENT = "package_name"

        fun show(activity: AppCompatActivity, name: String, packageName: String) = AppRequiredDialog().apply {
            arguments = bundleOf(NAME_ARGUMENT to name, PACKAGE_NAME_ARGUMENT to packageName)
        }.show(activity.supportFragmentManager, "app_required_dialog")
    }

    private val name get() = requireArguments().getString(NAME_ARGUMENT)
    private val packageName get() = requireArguments().getString(PACKAGE_NAME_ARGUMENT)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = MaterialDialog(requireContext())
        .title(text = getString(R.string.dialog_app_required_title, name))
        .message(text = getString(R.string.dialog_app_required_content, name))
        .positiveButton(R.string.dialog_app_required_positive) {
            try {
                requireContext().startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                )
            } catch (error: ActivityNotFoundException) {
                requireContext().startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
                )
            }
        }
        .negativeButton(R.string.cancel)
}
