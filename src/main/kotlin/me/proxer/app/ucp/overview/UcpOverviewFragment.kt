package me.proxer.app.ucp.overview

import android.content.ClipData
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.klinker.android.link_builder.Link
import com.klinker.android.link_builder.TouchableMovementMethod
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.util.Utils
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.clipboardManager
import me.proxer.app.util.extension.snackbar
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.util.ProxerUrls
import okhttp3.HttpUrl
import org.jetbrains.anko.bundleOf

/**
 * @author Ruben Gees
 */
class UcpOverviewFragment : BaseContentFragment<Int>() {

    companion object {
        private const val DATE_FORMAT = "%.1f"

        fun newInstance() = UcpOverviewFragment().apply {
            arguments = bundleOf()
        }
    }

    override val viewModel: UcpOverviewViewModel by unsafeLazy { UcpOverviewViewModelProvider.get(this) }

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

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        profileLink.movementMethod = TouchableMovementMethod.getInstance()
    }

    override fun showData(data: Int) {
        super.showData(data)

        StorageHelper.user?.let { (_, id, name) ->
            profileLink.text = Utils.buildClickableText(context, ProxerUrls.userWeb(id).toString(),
                    onWebClickListener = Link.OnClickListener {
                        HttpUrl.parse(it)?.let { showPage(it) }
                    },
                    onWebLongClickListener = Link.OnLongClickListener {
                        val title = getString(R.string.clipboard_title)

                        context.clipboardManager.primaryClip = ClipData.newPlainText(title, it)
                        snackbar(root, R.string.clipboard_status)
                    })

            username.text = name
            userId.text = id

            episodesRow.text = data.toString()

            val minutes = data * 20
            val hours = minutes / 60f
            val days = hours / 24f

            minutesRow.text = minutes.toString()
            hoursRow.text = DATE_FORMAT.format(hours)
            daysRow.text = DATE_FORMAT.format(days)
        }
    }
}
