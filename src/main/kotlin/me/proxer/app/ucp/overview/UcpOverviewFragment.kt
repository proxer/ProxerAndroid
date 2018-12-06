package me.proxer.app.ucp.overview

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import com.gojuno.koptional.Optional
import com.gojuno.koptional.rxjava2.filterSome
import com.gojuno.koptional.toOptional
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import kotterknife.bindView
import linkClicks
import linkLongClicks
import me.proxer.app.R
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.linkify
import me.proxer.app.util.extension.toast
import me.proxer.library.util.ProxerUrls
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author Ruben Gees
 */
class UcpOverviewFragment : BaseContentFragment<Optional<Int>>() {

    companion object {
        private const val DATE_FORMAT = "%.1f"

        fun newInstance() = UcpOverviewFragment().apply {
            arguments = bundleOf()
        }
    }

    override val viewModel by viewModel<UcpOverviewViewModel>()

    private val profileLink: TextView by bindView(R.id.profileLink)
    private val username: TextView by bindView(R.id.username)
    private val userId: TextView by bindView(R.id.userId)

    private val episodesRow: TextView by bindView(R.id.episodesRow)
    private val minutesRow: TextView by bindView(R.id.minutesRow)
    private val hoursRow: TextView by bindView(R.id.hoursRow)
    private val daysRow: TextView by bindView(R.id.daysRow)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_ucp_overview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        profileLink.linkClicks()
            .map { Utils.getAndFixUrl(it).toOptional() }
            .filterSome()
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe { showPage(it) }

        profileLink.linkLongClicks()
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe {
                val title = getString(R.string.clipboard_title)

                requireContext().getSystemService<ClipboardManager>()?.primaryClip = ClipData.newPlainText(title, it)
                requireContext().toast(R.string.clipboard_status)
            }
    }

    override fun showData(data: Optional<Int>) {
        super.showData(data)

        storageHelper.user?.let { (_, id, name) ->
            profileLink.text = ProxerUrls.userWeb(id).toString().linkify(mentions = false)

            username.text = name
            userId.text = id

            episodesRow.text = (data.toNullable() ?: 0).toString()

            val minutes = data.toNullable() ?: 0 * 20
            val hours = minutes / 60f
            val days = hours / 24f

            minutesRow.text = minutes.toString()
            hoursRow.text = DATE_FORMAT.format(hours)
            daysRow.text = DATE_FORMAT.format(days)
        }
    }
}
