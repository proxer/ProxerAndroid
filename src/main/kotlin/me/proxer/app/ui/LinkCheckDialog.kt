package me.proxer.app.ui

import android.app.Dialog
import android.os.Bundle
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.text.parseAsHtml
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.colorRes
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseDialog
import me.proxer.app.util.extension.getSafeString
import me.proxer.app.util.extension.openHttpPage
import me.proxer.app.util.extension.safeInject
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

class LinkCheckDialog : BaseDialog() {

    companion object {
        private const val LINK_ARGUMENT = "link"

        fun show(activity: FragmentActivity, link: HttpUrl) = LinkCheckDialog()
            .apply { arguments = bundleOf(LINK_ARGUMENT to link.toString()) }
            .show(activity.supportFragmentManager, "link_check_dialog")
    }

    private val viewModel by safeInject<LinkCheckViewModel>()

    private val text by bindView<TextView>(R.id.text)
    private val progress by bindView<ProgressBar>(R.id.progress)
    private val progressIcon by bindView<ImageView>(R.id.progressIcon)
    private val progressText by bindView<TextView>(R.id.progressText)
    private val remember by bindView<CheckBox>(R.id.remember)

    private val link: HttpUrl
        get() = requireArguments().getSafeString(LINK_ARGUMENT).toHttpUrl()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = MaterialDialog(requireContext())
        .customView(R.layout.dialog_link_check, scrollable = true)
        .positiveButton(R.string.dialog_link_check_positive) {
            if (remember.isChecked) {
                preferenceHelper.shouldCheckLinks = false
            }

            customTabsHelper.openHttpPage(requireActivity(), link)
        }
        .negativeButton(R.string.cancel)

    override fun onDialogCreated(savedInstanceState: Bundle?) {
        super.onDialogCreated(savedInstanceState)

        text.text = getString(R.string.dialog_link_check_message, link.toString()).parseAsHtml()

        viewModel.data.observe(
            dialogLifecycleOwner,
            Observer {
                if (it != null) {
                    if (it) {
                        progressText.setText(R.string.dialog_link_check_secure)

                        progressIcon.isVisible = true
                        progressIcon.setImageDrawable(
                            IconicsDrawable(requireContext(), CommunityMaterial.Icon3.cmd_shield_check).apply {
                                colorRes = R.color.green_500
                            }
                        )
                    } else {
                        progressText.setText(R.string.dialog_link_check_not_secure)

                        progressIcon.isVisible = true
                        progressIcon.setImageDrawable(
                            IconicsDrawable(requireContext(), CommunityMaterial.Icon3.cmd_shield_alert).apply {
                                colorRes = R.color.red_500
                            }
                        )
                    }
                }
            }
        )

        viewModel.isLoading.observe(
            dialogLifecycleOwner,
            Observer {
                progress.isVisible = it == true

                if (it == true) {
                    progressText.setText(R.string.dialog_link_check_progress)
                }
            }
        )

        if (savedInstanceState == null) {
            viewModel.check(link)
        }
    }
}
