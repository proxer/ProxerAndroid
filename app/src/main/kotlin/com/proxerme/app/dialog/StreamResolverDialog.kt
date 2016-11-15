package com.proxerme.app.dialog

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import com.proxerme.app.R
import com.proxerme.app.application.MainApplication
import com.proxerme.app.module.StreamResolvers
import com.proxerme.library.connection.anime.request.LinkRequest
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.runOnUiThread
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import java.util.concurrent.Future

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class StreamResolverDialog : DialogFragment() {

    companion object {

        private const val ID_ARGUMENT = "id"

        fun show(activity: AppCompatActivity, id: String) {
            StreamResolverDialog()
                    .apply { arguments = Bundle().apply { putString(ID_ARGUMENT, id) } }
                    .show(activity.supportFragmentManager, "dialog_stream_resolver")
        }
    }

    private val id: String
        get() = arguments.getString(ID_ARGUMENT)

    private var task: Future<Unit>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        retainInstance = true
        task = doAsync(exceptionHandler = {
            context.runOnUiThread {
                if (it.message.isNullOrBlank()) {
                    context.toast(context.getString(R.string.error_network))
                } else {
                    context.toast(it.message!!)
                }

                dismiss()
            }
        }, task = {
            val link = MainApplication.proxerConnection.executeSynchronized(LinkRequest(id))
            val result = StreamResolvers.getResolverFor(link)?.resolve(link)
                    ?: throw RuntimeException(getString(R.string.error_resolve))
            val uri = Uri.parse(result)

            uiThread {
                context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "video/mp4")
                })

                dismiss()
            }
        })
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialDialog.Builder(context)
                .autoDismiss(false)
                .title(getString(R.string.dialog_stream_resolver_title))
                .negativeText(getString(R.string.dialog_cancel))
                .onNegative({ materialDialog, dialogAction ->
                    materialDialog.cancel()
                })
                .customView(View.inflate(context, R.layout.dialog_progress, null), true)
                .build()
    }

    override fun onDestroy() {
        task?.cancel(true)
        task = null

        super.onDestroy()
    }
}