package me.proxer.app.anime

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import me.proxer.app.R
import me.proxer.app.base.BaseDialog
import org.jetbrains.anko.bundleOf

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

    private val name get() = arguments.getString(NAME_ARGUMENT)
    private val packageName get() = arguments.getString(PACKAGE_NAME_ARGUMENT)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = MaterialDialog.Builder(context)
            .title(getString(R.string.dialog_app_required_title, name))
            .content(getString(R.string.dialog_app_required_content, name))
            .positiveText(R.string.dialog_app_required_positive)
            .negativeText(R.string.cancel)
            .onPositive { _, _ ->
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=$packageName")))
                } catch (error: ActivityNotFoundException) {
                    context.startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
                }
            }
            .build()
}
