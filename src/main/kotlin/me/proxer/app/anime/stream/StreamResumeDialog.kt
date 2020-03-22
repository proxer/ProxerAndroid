package me.proxer.app.anime.stream

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.core.os.bundleOf
import com.afollestad.materialdialogs.MaterialDialog
import me.proxer.app.R
import me.proxer.app.base.BaseDialog

class StreamResumeDialog : BaseDialog() {

    companion object {
        private const val POSITION_ARGUMENT = "position"

        fun show(activity: StreamActivity, position: Long) = StreamResumeDialog()
            .apply { arguments = bundleOf(POSITION_ARGUMENT to position) }
            .show(activity.supportFragmentManager, "stream_resume_dialog")
    }

    private val position: Long
        get() = requireArguments().getLong(POSITION_ARGUMENT)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = MaterialDialog(requireContext())
        .message(text = getString(R.string.dialog_stream_resume_message, toTime(position)))
        .positiveButton(res = R.string.dialog_stream_resume_positive) {
            (requireActivity() as? StreamActivity)?.playContinue(position)
        }
        .negativeButton(res = R.string.dialog_stream_resume_negative) {
            (requireActivity() as? StreamActivity)?.playFromStart()
        }

    override fun onCancel(dialog: DialogInterface) {
        (requireActivity() as? StreamActivity)?.playFromStart()
    }

    private fun toTime(position: Long): String {
        val minutes = position / 1_000 / 60
        val seconds = (position - (minutes * 1000 * 60)) / 1000

        return "$minutes:${String.format("%02d", seconds)}"
    }
}
