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
import com.proxerme.app.event.LogoutEvent
import com.proxerme.app.helper.StorageHelper
import com.proxerme.app.task.ProxerLoadingTask
import com.proxerme.app.util.ErrorUtils
import com.proxerme.library.connection.ProxerException
import com.proxerme.library.connection.user.request.LogoutRequest
import org.greenrobot.eventbus.EventBus
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

    private val successCallback = { _: Void? ->
        StorageHelper.user = null

        EventBus.getDefault().post(LogoutEvent())

        dismiss()
    }

    private val exceptionCallback = { exception: Exception ->
        if (exception is ProxerException) {
            context.longToast(ErrorUtils.getMessageForErrorCode(context, exception))
        } else {
            context.longToast(R.string.error_unknown)
        }

        if(view != null){
            handleVisibility()
        }
    }

    private lateinit var task: ProxerLoadingTask<Unit, Void?>

    private lateinit var root: ViewGroup
    private lateinit var progress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        retainInstance = true

        task = ProxerLoadingTask({ LogoutRequest() }, successCallback, exceptionCallback)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialDialog.Builder(context)
                .autoDismiss(false)
                .title(R.string.dialog_logout_title)
                .positiveText(R.string.dialog_logout_positive)
                .negativeText(R.string.dialog_cancel)
                .onPositive({ _, _ ->
                    logout()
                })
                .onNegative({ materialDialog, _ ->
                    materialDialog.cancel()
                })
                .customView(initViews(), true)
                .build()
    }

    override fun onResume() {
        super.onResume()

        handleVisibility()
    }

    override fun onDestroy() {
        task.destroy()

        super.onDestroy()

        MainApplication.refWatcher.watch(this)
    }

    override fun onDestroyView() {
        if (dialog != null && retainInstance) {
            dialog.setDismissMessage(null)
        }

        super.onDestroyView()
    }

    private fun initViews(): View {
        root = View.inflate(context, R.layout.dialog_progress, null) as ViewGroup
        progress = root.findViewById(R.id.progress) as ProgressBar

        return root
    }

    private fun handleVisibility() {
        if (task.isWorking) {
            progress.visibility = View.VISIBLE
        } else {
            progress.visibility = View.GONE
        }
    }

    private fun logout() {
        if (!task.isWorking) {
            task.execute(Unit)

            handleVisibility()
        }
    }
}
