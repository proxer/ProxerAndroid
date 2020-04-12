package me.proxer.app.chat.pub.message

import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import me.proxer.app.chat.ReportDialog
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author Ruben Gees
 */
class ChatReportDialog : ReportDialog() {

    companion object {
        fun show(activity: AppCompatActivity, messageId: String) = ChatReportDialog().apply {
            arguments = bundleOf(ID_ARGUMENT to messageId)
        }.show(activity.supportFragmentManager, "chat_report_dialog")
    }

    override val viewModel by viewModel<ChatReportViewModel>()
}
