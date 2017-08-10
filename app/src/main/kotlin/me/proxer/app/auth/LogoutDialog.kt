package me.proxer.app.auth

import android.app.Dialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
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

    private val viewModel by unsafeLazy { ViewModelProviders.of(this).get(LogoutViewModel::class.java) }

    private val content: TextView by bindView(R.id.content)
    private val progress: ProgressBar by bindView(R.id.progress)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = MaterialDialog.Builder(context)
            .autoDismiss(false)
            .positiveText(R.string.dialog_logout_positive)
            .negativeText(R.string.cancel)
            .onPositive({ _, _ -> viewModel.logout() })
            .onNegative({ _, _ -> dismiss() })
            .customView(R.layout.dialog_logout, true)
            .build()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel.data.observe(this, Observer {
            it?.let { dismiss() }
        })

        viewModel.error.observe(this, Observer {
            it?.let {
                viewModel.error.value = null

                context.longToast(it.message)
            }
        })

        viewModel.isLoading.observe(this, Observer {
            content.visibility = if (it == true) View.GONE else View.VISIBLE
            progress.visibility = if (it == true) View.VISIBLE else View.GONE
        })
    }
}
