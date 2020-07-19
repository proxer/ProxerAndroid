package me.proxer.app.auth

import android.app.Dialog
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseDialog
import me.proxer.app.util.extension.toast
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author Ruben Gees
 */
class LogoutDialog : BaseDialog() {

    companion object {
        fun show(activity: FragmentActivity) = LogoutDialog().show(activity.supportFragmentManager, "logout_dialog")
    }

    private val viewModel by viewModel<LogoutViewModel>()

    private val content: TextView by bindView(R.id.content)
    private val progress: ProgressBar by bindView(R.id.progress)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = MaterialDialog(requireContext())
        .noAutoDismiss()
        .positiveButton(R.string.dialog_logout_positive) { viewModel.logout() }
        .negativeButton(R.string.cancel) { dismiss() }
        .customView(R.layout.dialog_logout, scrollable = true)

    override fun onDialogCreated(savedInstanceState: Bundle?) {
        super.onDialogCreated(savedInstanceState)

        viewModel.success.observe(
            dialogLifecycleOwner,
            Observer {
                it?.let { dismiss() }
            }
        )

        viewModel.error.observe(
            dialogLifecycleOwner,
            Observer {
                it?.let {
                    viewModel.error.value = null

                    requireContext().toast(it.message)
                }
            }
        )

        viewModel.isLoading.observe(
            dialogLifecycleOwner,
            Observer {
                content.isGone = it == true
                progress.isVisible = it == true
            }
        )
    }
}
