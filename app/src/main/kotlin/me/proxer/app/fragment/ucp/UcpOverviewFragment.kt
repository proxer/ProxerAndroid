package me.proxer.app.fragment.ucp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.klinker.android.link_builder.TouchableMovementMethod
import com.proxerme.library.api.ProxerCall
import com.proxerme.library.util.ProxerUrls
import me.proxer.app.R
import me.proxer.app.fragment.base.LoadingFragment
import me.proxer.app.helper.StorageHelper
import me.proxer.app.task.ProxerTask
import me.proxer.app.util.extension.api
import me.proxer.app.util.extension.bindView

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class UcpOverviewFragment : LoadingFragment<ProxerCall<Int>, Int>() {

    companion object {
        private const val FORMAT = "%.1f"

        fun newInstance(): UcpOverviewFragment {
            return UcpOverviewFragment()
        }
    }

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

    override fun onSuccess(result: Int) {
        super.onSuccess(result)

        StorageHelper.user?.let {
            profileLink.text = ProxerUrls.userWeb(it.id).toString()

//                    Utils.buildClickableText(context,
//                    ProxerUrlHolder.getUserUrl(it.id, null).toString(),
//                    onWebClickListener = Link.OnClickListener { link ->
//                        showPage(Utils.parseAndFixUrl(link))
//                    },
//                    onWebLongClickListener = Link.OnLongClickListener { link ->
//                        Utils.setClipboardContent(activity,
//                                getString(R.string.fragment_ucp_overview_clip_title), link)
//
//                        context.toast(R.string.clipboard_status)
//                    })

            username.text = it.name
            userId.text = it.id

            episodesRow.text = result.toString()

            val minutes = result * 20
            val hours = minutes / 60f
            val days = hours / 24f

            minutesRow.text = minutes.toString()
            hoursRow.text = FORMAT.format(hours)
            daysRow.text = FORMAT.format(days)
        }
    }

    override fun constructTask() = ProxerTask<Int>()
    override fun constructInput() = api.ucp().watchedEpisodes().build()
}
