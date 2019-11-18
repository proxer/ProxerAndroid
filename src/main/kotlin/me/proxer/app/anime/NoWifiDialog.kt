package me.proxer.app.anime

import android.app.Dialog
import android.os.Bundle
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseDialog
import me.proxer.app.util.extension.getSafeString
import me.proxer.app.util.extension.unsafeLazy
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author Ruben Gees
 */
class NoWifiDialog : BaseDialog() {

    companion object {
        private const val STREAM_ID_ARGUMENT = "stream_id"

        fun show(activity: AppCompatActivity, fragment: Fragment, streamId: String) = NoWifiDialog()
            .apply { arguments = bundleOf(STREAM_ID_ARGUMENT to streamId) }
            .apply { setTargetFragment(fragment, 0) }
            .show(activity.supportFragmentManager, "no_wifi_dialog")
    }

    private val viewModel by unsafeLazy { requireTargetFragment().viewModel<AnimeViewModel>().value }

    private val remember by bindView<CheckBox>(R.id.remember)

    private val streamId: String
        get() = requireArguments().getSafeString(STREAM_ID_ARGUMENT)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = MaterialDialog(requireContext())
        .customView(R.layout.dialog_no_wifi, scrollable = true)
        .positiveButton(R.string.dialog_no_wifi_positive) {
            (requireTargetFragment() as AnimeFragment).onConfirmNoWifi(streamId, remember.isChecked)
        }
        .negativeButton(R.string.cancel)
}
