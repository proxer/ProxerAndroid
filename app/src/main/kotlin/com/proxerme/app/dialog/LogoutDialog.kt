package com.proxerme.app.dialog

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import com.afollestad.materialdialogs.MaterialDialog
import com.proxerme.app.R
import com.proxerme.app.manager.UserManager
import com.proxerme.app.util.ErrorHandler
import org.jetbrains.anko.longToast

/**
 * Dialog, which handles the logout of a user.

 * @author Ruben Gees
 */
class LogoutDialog : DialogFragment() {

    companion object {
        private const val STATE_LOADING = "dialog_logout_state_loading"

        fun show(activity: AppCompatActivity) {
            LogoutDialog().show(activity.supportFragmentManager, "dialog_logout")
        }
    }

    private var loading: Boolean = false

    private lateinit var root: ViewGroup
    private lateinit var progress: ProgressBar

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialDialog.Builder(context)
                .autoDismiss(false)
                .title(R.string.dialog_logout_title)
                .positiveText(R.string.dialog_logout_positive)
                .negativeText(R.string.dialog_cancel)
                .onPositive({ materialDialog, dialogAction ->
                    logout()
                })
                .onNegative({ materialDialog, dialogAction ->
                    materialDialog.cancel()
                })
                .customView(initViews(), true)
                .build()
    }

    override fun onDestroy() {
        UserManager.cancel()

        super.onDestroy()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (savedInstanceState != null) {
            loading = savedInstanceState.getBoolean(STATE_LOADING)
        }

        handleVisibility()
    }

    private fun initViews(): View {
        root = View.inflate(context, R.layout.dialog_logout, null) as ViewGroup
        progress = root.findViewById(R.id.progress) as ProgressBar

        return root
    }

    private fun handleVisibility() {
        if (loading) {
            progress.visibility = View.VISIBLE
        } else {
            progress.visibility = View.GONE
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBoolean(STATE_LOADING, loading)
    }

    private fun logout() {
        if (!loading) {
            loading = true
            handleVisibility()

            UserManager.logout({
                loading = false

                dismiss()
            }, { result ->
                loading = false

                handleVisibility()
                context.longToast(ErrorHandler.getMessageForErrorCode(context, result))
            })
        }
    }
}
