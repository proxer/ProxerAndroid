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
import com.proxerme.app.application.MainApplication
import com.proxerme.app.event.LogoutFailedEvent
import com.proxerme.app.manager.UserManager
import com.proxerme.app.util.ErrorHandler
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.longToast

/**
 * Dialog, which handles the logout of a user.

 * @author Ruben Gees
 */
class LogoutDialog : DialogFragment() {

    companion object {
        fun show(activity: AppCompatActivity) {
            LogoutDialog().show(activity.supportFragmentManager, "dialog_logout")
        }
    }

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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (UserManager.user == null) {
            dismiss()
        } else {
            handleVisibility()
        }
    }

    override fun onStart() {
        super.onStart()

        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)

        super.onStop()
    }

    override fun onDestroy() {
        if (activity != null && !activity.isChangingConfigurations) {
            UserManager.cancel()
        }

        super.onDestroy()

        MainApplication.refWatcher.watch(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLoginStateChanged(@Suppress("UNUSED_PARAMETER") state: UserManager.LoginState) {
        if (UserManager.user == null) {
            dismiss()
        } else {
            handleVisibility()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOngoingStateChanged(@Suppress("UNUSED_PARAMETER") state: UserManager.OngoingState) {
        handleVisibility()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLoginFailed(event: LogoutFailedEvent) {
        context.longToast(ErrorHandler.getMessageForErrorCode(context, event.exception))
    }

    private fun initViews(): View {
        root = View.inflate(context, R.layout.dialog_logout, null) as ViewGroup
        progress = root.findViewById(R.id.progress) as ProgressBar

        return root
    }

    private fun handleVisibility() {
        if (UserManager.ongoingState == UserManager.OngoingState.LOGGING_OUT) {
            progress.visibility = View.VISIBLE
        } else {
            progress.visibility = View.GONE
        }
    }

    private fun logout() {
        if (UserManager.ongoingState != UserManager.OngoingState.LOGGING_OUT) {
            UserManager.logout()
        }
    }
}
