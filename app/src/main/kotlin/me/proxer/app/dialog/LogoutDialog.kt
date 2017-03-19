package me.proxer.app.dialog

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.view.View
import android.widget.ProgressBar
import com.afollestad.materialdialogs.MaterialDialog
import com.rubengees.ktask.android.bindToLifecycle
import com.rubengees.ktask.util.TaskBuilder
import me.proxer.app.R
import me.proxer.app.activity.MainActivity
import me.proxer.app.application.MainApplication
import me.proxer.app.dialog.base.MainDialog
import me.proxer.app.event.LogoutEvent
import me.proxer.app.helper.StorageHelper
import me.proxer.app.task.ProxerTask
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.extension.api
import me.proxer.app.util.extension.bindView
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.longToast

/**
 * @author Ruben Gees
 */
class LogoutDialog : MainDialog() {

    companion object {
        fun show(activity: FragmentActivity) {
            LogoutDialog().show(activity.supportFragmentManager, "dialog_logout")
        }
    }

    private val task by lazy {
        TaskBuilder.task(ProxerTask<Void?>())
                .bindToLifecycle(this)
                .onInnerStart {
                    setProgressVisible(true)
                }
                .onSuccess {
                    StorageHelper.user = null

                    EventBus.getDefault().post(LogoutEvent())

                    dismiss()
                }
                .onError {
                    val action = ErrorUtils.handle(activity as MainActivity, it)

                    context.longToast(action.message)
                }
                .onFinish {
                    setProgressVisible(false)
                }.build()
    }

    private val progress: ProgressBar by bindView(R.id.progress)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialDialog.Builder(context)
                .autoDismiss(false)
                .title(R.string.dialog_logout_title)
                .positiveText(R.string.dialog_logout_positive)
                .negativeText(R.string.dialog_cancel)
                .onPositive({ _, _ ->
                    task.execute(api.user().logout().build())
                })
                .onNegative({ _, _ ->
                    dismiss()
                })
                .customView(R.layout.dialog_progress, true)
                .build()
    }

    override fun onResume() {
        super.onResume()

        setProgressVisible(task.isWorking)
    }

    override fun onDestroy() {
        super.onDestroy()

        MainApplication.refWatcher.watch(this)
    }

    private fun setProgressVisible(visible: Boolean) {
        progress.visibility = when (visible) {
            true -> View.VISIBLE
            false -> View.GONE
        }
    }
}
