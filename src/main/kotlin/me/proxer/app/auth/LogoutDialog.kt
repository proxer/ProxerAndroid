package me.proxer.app.auth

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseDialog
import me.proxer.app.util.extension.unsafeLazy
import org.jetbrains.anko.longToast

/**
 * @author Ruben Gees
 */
class LogoutDialog : BaseDialog() {

    companion object {
        fun show(activity: FragmentActivity) = LogoutDialog().show(activity.supportFragmentManager, "logout_dialog")
    }

    private val viewModel by unsafeLazy { LogoutViewModelProvider.get(this) }

    private val content: TextView by bindView(R.id.content)
    private val progress: ProgressBar by bindView(R.id.progress)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = MaterialDialog(requireContext())
        .noAutoDismiss()
        .positiveButton(R.string.dialog_logout_positive) { viewModel.logout() }
        .negativeButton(R.string.cancel) { dismiss() }
        .customView(R.layout.dialog_logout, scrollable = true)

    override fun onDialogCreated(savedInstanceState: Bundle?) {
        super.onDialogCreated(savedInstanceState)

        viewModel.data.observe(dialogLifecycleOwner, Observer {
            it?.let { _ -> dismiss() }
        })

        viewModel.error.observe(dialogLifecycleOwner, Observer {
            it?.let { _ ->
                viewModel.error.value = null

                requireContext().longToast(it.message)
            }
        })

        viewModel.isLoading.observe(dialogLifecycleOwner, Observer {
            content.visibility = if (it == true) View.GONE else View.VISIBLE
            progress.visibility = if (it == true) View.VISIBLE else View.GONE
        })
    }
}
