package me.proxer.app.fragment.ucp

import android.content.ClipData
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.klinker.android.link_builder.Link
import com.klinker.android.link_builder.TouchableMovementMethod
import com.rubengees.ktask.util.TaskBuilder
import me.proxer.app.R
import me.proxer.app.application.MainApplication.Companion.api
import me.proxer.app.fragment.base.LoadingFragment
import me.proxer.app.helper.StorageHelper
import me.proxer.app.task.asyncProxerTask
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.bindView
import me.proxer.app.util.extension.clipboardManager
import me.proxer.app.util.extension.snackbar
import me.proxer.library.api.ProxerCall
import me.proxer.library.util.ProxerUrls
import okhttp3.HttpUrl
import org.jetbrains.anko.bundleOf

/**
 * @author Ruben Gees
 */
class UcpOverviewFragment : LoadingFragment<ProxerCall<Int>, Int>() {

    companion object {
        private const val DATE_FORMAT = "%.1f"

        fun newInstance(): UcpOverviewFragment {
            return UcpOverviewFragment().apply {
                arguments = bundleOf()
            }
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

        val user = StorageHelper.user

        if (user != null) {
            profileLink.text = Utils.buildClickableText(context, ProxerUrls.userWeb(user.id).toString(),
                    onWebClickListener = Link.OnClickListener {
                        HttpUrl.parse(it)?.let { showPage(it) }
                    },
                    onWebLongClickListener = Link.OnLongClickListener {
                        val title = getString(R.string.clipboard_title)

                        context.clipboardManager.primaryClip = ClipData.newPlainText(title, it)
                        snackbar(root, R.string.clipboard_status)
                    })

            username.text = user.name
            userId.text = user.id

            episodesRow.text = result.toString()

            val minutes = result * 20
            val hours = minutes / 60f
            val days = hours / 24f

            minutesRow.text = minutes.toString()
            hoursRow.text = DATE_FORMAT.format(hours)
            daysRow.text = DATE_FORMAT.format(days)
        }
    }

    override fun constructTask() = TaskBuilder.asyncProxerTask<Int>().build()
    override fun constructInput() = api.ucp().watchedEpisodes().build()
}
