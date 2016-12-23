package com.proxerme.app.fragment.ucp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.klinker.android.link_builder.Link
import com.klinker.android.link_builder.TouchableMovementMethod
import com.proxerme.app.R
import com.proxerme.app.fragment.framework.SingleLoadingFragment
import com.proxerme.app.manager.SectionManager.Section
import com.proxerme.app.manager.UserManager
import com.proxerme.app.task.ProxerLoadingTask
import com.proxerme.app.task.framework.ListenableTask
import com.proxerme.app.util.Utils
import com.proxerme.app.util.bindView
import com.proxerme.library.connection.ucp.request.ListsumRequest
import com.proxerme.library.info.ProxerUrlHolder
import org.jetbrains.anko.toast

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class UcpOverviewFragment : SingleLoadingFragment<Unit, Int>() {

    companion object {
        private const val FORMAT = "%.1f"

        fun newInstance(): UcpOverviewFragment {
            return UcpOverviewFragment()
        }
    }

    override val section = Section.UCP_OVERVIEW
    override val isLoginRequired = true

    private val profileLink: TextView by bindView(R.id.profileLink)
    private val username: TextView by bindView(R.id.username)
    private val userId: TextView by bindView(R.id.userId)

    private val episodesRow: TextView by bindView(R.id.episodesRow)
    private val minutesRow: TextView by bindView(R.id.minutesRow)
    private val hoursRow: TextView by bindView(R.id.hoursRow)
    private val daysRow: TextView by bindView(R.id.daysRow)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_ucp_overview, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        profileLink.movementMethod = TouchableMovementMethod.getInstance()
    }

    override fun constructTask(): ListenableTask<Unit, Int> {
        return ProxerLoadingTask({ ListsumRequest() })
    }

    override fun constructInput() {
        return Unit
    }

    override fun present(data: Int) {
        UserManager.user?.let {
            profileLink.text = Utils.buildClickableText(context,
                    ProxerUrlHolder.getUserUrl(it.id, null).toString(),
                    onWebClickListener = Link.OnClickListener { link ->
                        showPage(Utils.parseAndFixUrl(link))
                    },
                    onWebLongClickListener = Link.OnLongClickListener { link ->
                        Utils.setClipboardContent(activity,
                                getString(R.string.fragment_ucp_overview_clip_title), link)

                        context.toast(R.string.clipboard_status)
                    })
            username.text = it.username
            userId.text = it.id

            episodesRow.text = data.toString()

            val minutes = data * 20
            val hours = minutes / 60f
            val days = hours / 24f

            minutesRow.text = minutes.toString()
            hoursRow.text = FORMAT.format(hours)
            daysRow.text = FORMAT.format(days)
        }
    }
}