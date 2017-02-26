package com.proxerme.app.dialog

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.ProgressBar
import com.afollestad.materialdialogs.MaterialDialog
import com.proxerme.app.activity.MainActivity
import com.proxerme.app.application.MainApplication
import com.proxerme.app.event.LogoutEvent
import com.proxerme.app.helper.StorageHelper
import com.proxerme.app.task.ProxerLoadingTask
import com.proxerme.app.util.ErrorUtils
import com.proxerme.app.util.KotterKnife
import com.proxerme.app.util.bindView
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
        val action = ErrorUtils.handle(activity as MainActivity, exception)

        context.longToast(action.message)

        if (dialog != null) {
            handleVisibility()
        }
    }

    private lateinit var task: ProxerLoadingTask<Unit, Void?>

    private val progress: ProgressBar by bindView(R.id.progress)

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
                    if (!task.isWorking) {
                        task.execute(Unit)

                        handleVisibility()
                    }
                })
                .onNegative({ materialDialog, _ ->
                    materialDialog.cancel()
                })
                .customView(R.layout.dialog_progress, true)
                .build()
    }

    override fun onStart() {
        super.onStart()

        handleVisibility()
    }

    override fun onDestroyView() {
        dialog?.setDismissMessage(null)
        KotterKnife.reset(this)

        super.onDestroyView()
    }

    override fun onDestroy() {
        task.destroy()

        super.onDestroy()

        MainApplication.refWatcher.watch(this)
    }

    private fun handleVisibility() {
        progress.visibility = when (task.isWorking) {
            true -> View.VISIBLE
            false -> View.GONE
        }
    }
}
