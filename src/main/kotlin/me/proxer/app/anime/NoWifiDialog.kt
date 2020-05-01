package me.proxer.app.anime

import android.app.Dialog
import android.os.Bundle
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseDialog
import me.proxer.app.util.extension.getSafeString

/**
 * @author Ruben Gees
 */
class NoWifiDialog : BaseDialog() {

    companion object {
        const val STREAM_ID_RESULT = "stream_id"

        private const val STREAM_ID_ARGUMENT = "stream_id"

        fun show(activity: AppCompatActivity, streamId: String) = NoWifiDialog()
            .apply { arguments = bundleOf(STREAM_ID_ARGUMENT to streamId) }
            .show(activity.supportFragmentManager, "no_wifi_dialog")
    }

    private val remember by bindView<CheckBox>(R.id.remember)

    private val streamId: String
        get() = requireArguments().getSafeString(STREAM_ID_ARGUMENT)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = MaterialDialog(requireContext())
        .customView(R.layout.dialog_no_wifi, scrollable = true)
        .positiveButton(R.string.dialog_no_wifi_positive) {
            if (remember.isChecked) {
                preferenceHelper.shouldCheckCellular = false
            }

            setFragmentResult(STREAM_ID_RESULT, bundleOf(STREAM_ID_RESULT to streamId))
        }
        .negativeButton(R.string.cancel)
}
