package me.proxer.app.chat.prv.message

import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import me.proxer.app.chat.ReportDialog
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author Ruben Gees
 */
class MessengerReportDialog : ReportDialog() {

    companion object {
        fun show(activity: AppCompatActivity, conferenceId: String) = MessengerReportDialog().apply {
            arguments = bundleOf(ID_ARGUMENT to conferenceId)
        }.show(activity.supportFragmentManager, "messenger_report_dialog")
    }

    override val viewModel by viewModel<MessengerReportViewModel>()
}
