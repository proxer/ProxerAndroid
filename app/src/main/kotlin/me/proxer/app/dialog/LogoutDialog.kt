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
import me.proxer.app.activity.base.MainActivity
import me.proxer.app.application.MainApplication.Companion.api
import me.proxer.app.dialog.base.MainDialog
import me.proxer.app.task.asyncProxerTask
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.extension.bindView
import org.jetbrains.anko.longToast

/**
 * @author Ruben Gees
 */
class LogoutDialog : MainDialog() {

    companion object {
        fun show(activity: FragmentActivity) {
            LogoutDialog().show(activity.supportFragmentManager, "logout_dialog")
        }
    }

    private val task by lazy {
        TaskBuilder.asyncProxerTask<Void?>()
                .bindToLifecycle(this)
                .onInnerStart {
                    setProgressVisible(true)
                }
                .onSuccess {
                    dismiss()
                }
                .onError {
                    ErrorUtils.handle(activity as MainActivity, it).let {
                        context.longToast(it.message)
                    }
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
                .negativeText(R.string.cancel)
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

    private fun setProgressVisible(visible: Boolean) {
        progress.visibility = when (visible) {
            true -> View.VISIBLE
            false -> View.GONE
        }
    }
}
